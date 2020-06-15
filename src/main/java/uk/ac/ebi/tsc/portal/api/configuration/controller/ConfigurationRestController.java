package uk.ac.ebi.tsc.portal.api.configuration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.service.AccountService;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.application.service.ApplicationService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopy;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParametersNotFoundException;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParametersService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyNotFoundException;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyService;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigDeploymentParamsCopy;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;
import uk.ac.ebi.tsc.portal.api.configuration.service.*;
import uk.ac.ebi.tsc.portal.api.deployment.controller.DeploymentResource;
import uk.ac.ebi.tsc.portal.api.deployment.controller.DeploymentRestController;
import uk.ac.ebi.tsc.portal.api.deployment.repo.Deployment;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.encryptdecrypt.security.EncryptionService;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;
import uk.ac.ebi.tsc.portal.clouddeployment.utils.NamesPatternMatcher;
import uk.ac.ebi.tsc.portal.usage.deployment.service.DeploymentIndexService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.QueryParam;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@RestController
@RequestMapping(value = "/configuration", produces = {MediaType.APPLICATION_JSON_VALUE})
public class ConfigurationRestController {

	private static final Logger logger = LoggerFactory.getLogger(ConfigurationRestController.class);

	private final ConfigurationService configurationService;
	private final AccountService accountService;
	private final ApplicationService applicationService;
	private final CloudProviderParametersService cloudProviderParametersService;
	private final ConfigurationDeploymentParametersService configurationDeploymentParametersService;
	private final ConfigurationDeploymentParameterService configurationDeploymentParameterService;
	private final DeploymentService deploymentService;
	private uk.ac.ebi.tsc.aap.client.security.TokenHandler tokenHandler;
	private final DeploymentRestController deploymentRestController;
	private final CloudProviderParamsCopyService cloudProviderParametersCopyService;
	private final ConfigDeploymentParamsCopyService configDeploymentParamsCopyService;
	private final ConfigDeploymentParamCopyService configDeploymentParamCopyService;
	
    @Value("${elasticsearch.url}")
    private String elasticSearchUrl;

    @Value("${elasticsearch.index}")
    private String elasticSearchIndex;

    @Value("${elasticsearch.username}")
    private String elasticSearchUsername;

    @Value("${elasticsearch.password}")
    private String elasticSearchPassword;
	
	@Autowired
	public ConfigurationRestController(ConfigurationService configurationService,
			AccountService accountService,
			CloudProviderParametersService cloudProviderParametersService,
			ConfigurationDeploymentParametersService configurationDeploymentParametersService,
			ConfigurationDeploymentParameterService configurationDeploymentParameterService,
			DomainService domainService,
									   uk.ac.ebi.tsc.aap.client.security.TokenHandler tokenHandler,
			DeploymentService deploymentService,
			DeploymentRestController deploymentRestController,
			CloudProviderParamsCopyService cloudProviderParametersCopyService,
			ConfigDeploymentParamsCopyService configDeploymentParamsCopyService,
			ConfigDeploymentParamCopyService configDeploymentParamCopyService,
			EncryptionService encryptionService,
			ApplicationService applicationService
			) {
		this.accountService = accountService;
		this.cloudProviderParametersCopyService = cloudProviderParametersCopyService;
		this.cloudProviderParametersService = cloudProviderParametersService;
		this.configurationDeploymentParametersService = configurationDeploymentParametersService;
		this.configurationDeploymentParameterService = configurationDeploymentParameterService;
		this.deploymentService = deploymentService;
		this.configurationService = configurationService;
		this.tokenHandler = tokenHandler;
		this.deploymentRestController = deploymentRestController;
		this.configDeploymentParamsCopyService = configDeploymentParamsCopyService;
		this.configDeploymentParamCopyService = configDeploymentParamCopyService;
		this.applicationService = applicationService;
	}



	@RequestMapping(method = {RequestMethod.GET, RequestMethod.OPTIONS} )
	public Resources<ConfigurationResource> getCurrentUserConfigurations(@RequestParam(name = "applicationReference",
			required = false) String applicationReference,Principal principal) {

		logger.info("Account " + principal.getName() + " configurations requested; Filter applicationReference : "+applicationReference);

		Account account = this.accountService.findByUsername(principal.getName());

		Collection<Configuration> configurations = this.configurationService.findByAccountUsername(account.getUsername());
		logger.info(configurations.size() + " User configuration resources found ");
		List<ConfigurationResource> configurationResourceList;
		//Filter configuration, send only configuration that is applicable if applicationReference is passed
		if (applicationReference != null && !applicationReference.equals("")) {
			configurations = filterConfigurationByApplication(configurations, applicationReference, account);
		}
		configurationResourceList = this.configurationService.createConfigurationResource(configurations);
		configurationResourceList = configurationService.checkObsoleteConfigurations(configurationResourceList, account, this.configDeploymentParamsCopyService);
		logger.info("Got current user configurations  " + configurationResourceList.size());
		return new Resources<>(configurationResourceList);

	}


	private Set<Configuration> filterConfigurationByApplication(Collection<Configuration> configurations, String applicationReference, Account account){
		Application theApplication = applicationService.findByReference(applicationReference);
		Set<Configuration> configurationSet = new HashSet<>();
		for (Configuration configuration : configurations) {
			if (configurationService.canConfigurationBeUsedForApplication(configuration, theApplication, account)) {
				configurationSet.add(configuration);
			}
		}
		return configurationSet;
	}

	@RequestMapping(method = { RequestMethod.POST, RequestMethod.OPTIONS } )
	public ResponseEntity<?> add(Principal principal, @RequestBody ConfigurationResource input) {

		logger.info("Account " + principal.getName() + " configuration " + input.getName() + " addition requested");

		Account account = this.accountService.findByUsername(principal.getName());

		// Check configuration name isn't empty
		if (input.getName()==null || input.getName().length()<1) {
			throw new InvalidConfigurationInputException(account.getUsername(), input.getName());
		}

		if(!NamesPatternMatcher.nameMatchesPattern(input.getName())){
			logger.error("Input configuration name does not match required name pattern");
			throw new InvalidConfigurationInputException(input.getName());
		}

		// Check the cloudproviderparameters do exist
		CloudProviderParameters cloudProviderParameters = null;
		try{
			cloudProviderParameters = this.cloudProviderParametersService.findByNameAndAccountUsername(
					input.getCloudProviderParametersName(), principal.getName());
		}catch(CloudProviderParametersNotFoundException e){
			cloudProviderParameters = this.cloudProviderParametersService.findByName(input.getCloudProviderParametersName());
			Set<String> sharedWithTeamsDomainReferences = cloudProviderParameters.getSharedWithTeams()
					.stream().map(Team::getDomainReference).collect(Collectors.toSet());
			if(account.getMemberOfTeams().stream()
					.map(Team::getDomainReference)
					.filter(t -> sharedWithTeamsDomainReferences.contains(t))
					.collect(Collectors.toList()).isEmpty()){
				cloudProviderParameters = null;
			}
		}

		if (cloudProviderParameters == null) {
			throw new CloudProviderParametersNotFoundException(account.getUsername());
		}


		ConfigurationDeploymentParameters deploymentParameters = this.configurationDeploymentParametersService.findByName(input.getDeploymentParametersName());
		//get the deployment parameters 	
		ConfigDeploymentParamsCopy deploymentParametersCopy = 
				this.configDeploymentParamsCopyService.findByConfigurationDeploymentParametersReference(deploymentParameters.getReference());

		// Create model entity
		Configuration configuration = new Configuration(
				input.getName(), account, cloudProviderParameters.getName(),
				cloudProviderParameters.getReference(),
				input.getSshKey(),
				input.getSoftUsageLimit(),
				input.getHardUsageLimit(),
				deploymentParametersCopy);

		//Persist deployment parameters
		deploymentParametersCopy = this.configDeploymentParamsCopyService.save(deploymentParametersCopy);

		UUID reference = UUID.randomUUID();
		configuration.setReference(reference.toString());

		// Persist
		Configuration savedConfiguration = this.configurationService.save(configuration);
		CloudProviderParamsCopy cppCopy = cloudProviderParametersCopyService.findByCloudProviderParametersReference(configuration.getCloudProviderParametersReference());
		// Prepare response
		HttpHeaders httpHeaders = new HttpHeaders();

		return new ResponseEntity<>(new ConfigurationResource(savedConfiguration, cppCopy), httpHeaders, HttpStatus.CREATED);

	}

	@RequestMapping(value = "/{name:.+}", method = {RequestMethod.PUT})
	public ResponseEntity<?> update(Principal principal, @RequestBody ConfigurationResource input, @PathVariable("name") String name){

		logger.info("Account " + principal.getName() + " configuration " + input.getName() + " update requested");
		Account account = this.accountService.findByUsername(principal.getName());
		Configuration configuration = configurationService.update(account, principal, input, name, configDeploymentParamsCopyService);
		CloudProviderParamsCopy cppCopy = cloudProviderParametersCopyService.findByCloudProviderParametersReference(configuration.getCloudProviderParametersReference());
		
		// Prepare response
		HttpHeaders httpHeaders = new HttpHeaders();
		return new ResponseEntity<>(new ConfigurationResource(configuration, cppCopy), httpHeaders, HttpStatus.OK);
	}

	@RequestMapping(value = "/{name:.+}", method = { RequestMethod.GET } )
	public Resource<ConfigurationResource> getConfigurationByName(Principal principal, @PathVariable("name") String name)  {

		logger.info("Account " + principal.getName() + " configuration " + name + "  requested");

		Configuration configuration = this.configurationService.findByNameAndAccountUsername(name, principal.getName());
		CloudProviderParamsCopy cppCopy = cloudProviderParametersCopyService.findByCloudProviderParametersReference(configuration.getCloudProviderParametersReference());
		ConfigDeploymentParamsCopy cdpCopy = this.configDeploymentParamsCopyService.findByConfigurationDeploymentParametersReference(configuration.getConfigDeployParamsReference());

		ConfigurationResource configurationResource = 
				new ConfigurationResource(configuration, cppCopy);

		return new Resource<>(configurationResource);
	}

	@RequestMapping(value = "/{name:.+}", method = { RequestMethod.DELETE } )
	public ResponseEntity<?> delete(Principal principal, @PathVariable("name") String name) throws IOException   {

		logger.info("Account " + principal.getName() + " configuration " + name + " deletion requested");

		Configuration configuration = this.configurationService.findByNameAndAccountUsername(name, principal.getName());
		String cppReference = configuration.getCloudProviderParametersReference();
		String cdpsReference = configuration.getConfigDeployParamsReference();
		//stop all deployments using the configuration
		this.configurationService.stopDeploymentsOnDeletionOfConfiguration(name, principal, deploymentService, deploymentRestController, configuration);
		this.configurationService.delete(configuration);

		logger.info("Check and delete copies of cloud provider and deployment configuration parameter,\n"
				+ "if there are no longer used");
		//delete cpp, cdp copy if it is not referenced by deployment or configuration
		this.configurationService.deleteCPPAndCDPCopies(cppReference, cdpsReference, 
				this.deploymentService, this.configDeploymentParamsCopyService);


		// Prepare response
		HttpHeaders httpHeaders = new HttpHeaders();

		return new ResponseEntity<>(null, httpHeaders, HttpStatus.OK);
	}

	@RequestMapping(value = "/deploymentparameters/{name:.+}", method = {RequestMethod.GET, RequestMethod.OPTIONS} )
	public Resource<ConfigurationDeploymentParametersResource> getDeploymentParametersByName(Principal principal, @PathVariable("name") String name) {

		logger.info("Account " + principal.getName() + " deployment parameters requested");

		Account account = this.accountService.findByUsername(principal.getName());

		ConfigurationDeploymentParameters deploymentParameters = this.configurationDeploymentParametersService
				.findByNameAndAccountUserName(name, account.getUsername());

		ConfigurationDeploymentParametersResource configurationDeploymentParametersResource = 
				new ConfigurationDeploymentParametersResource(deploymentParameters);
		configurationDeploymentParametersResource.getFields().forEach(f -> {
			logger.info(f.getKey() + " " + f.getValue());
		}
				);
		return new Resource<>(configurationDeploymentParametersResource);

	}

	@RequestMapping(value = "/deploymentparameters", method = {RequestMethod.GET, RequestMethod.OPTIONS} )
	public Resources<ConfigurationDeploymentParametersResource> getAllDeploymentParameters(Principal principal) {

		logger.info("Account " + principal.getName() + " configurations requested");

		Account account = this.accountService.findByUsername(principal.getName());

		Collection<ConfigurationDeploymentParameters> deploymentParameters = 
				this.configurationDeploymentParametersService.findByAccountUsername(account.getUsername());

		List<ConfigurationDeploymentParametersResource> configurationDeploymentParametersResourceList =
				deploymentParameters
				.stream()
				.map(ConfigurationDeploymentParametersResource::new)
				.collect(Collectors.toList());
		return new Resources<>(configurationDeploymentParametersResourceList);

	}

	@RequestMapping(value = "/deploymentparameters", method = {RequestMethod.POST, RequestMethod.OPTIONS} )
	public ResponseEntity<?> addDeploymentParameters(Principal principal, @RequestBody ConfigurationDeploymentParametersResource input) {

		logger.info("Account " + principal.getName() + " configuration " + input.getName() + " addition requested");

		Account account = this.accountService.findByUsername(principal.getName());

		// Check configuration deployment parameters name isn't empty
		if (input.getName()==null || input.getName().length()<1) {
			throw new InvalidConfigurationDeploymentParametersException(account.getUsername(), input.getName());
		}

		if(!NamesPatternMatcher.nameMatchesPattern(input.getName())){
			logger.error("Input configuration deployment parameters name does not match required name pattern");
			throw new InvalidConfigurationDeploymentParametersException(input.getName());
		}

		ConfigurationDeploymentParameters savedConfigurationDeploymentParameters = this.configurationService.createNewDeploymentParameters(principal, account, input, 
				configurationDeploymentParameterService, configDeploymentParamsCopyService, configDeploymentParamCopyService
				);

		// Prepare response
		HttpHeaders httpHeaders = new HttpHeaders();

		return new ResponseEntity<>(new ConfigurationDeploymentParametersResource(savedConfigurationDeploymentParameters), httpHeaders, HttpStatus.CREATED);
	}

	@RequestMapping(value = "/deploymentparameters/{name:.+}", method = {RequestMethod.PUT} )
	public ResponseEntity<?> updateDeploymentParameters(Principal principal, @RequestBody ConfigurationDeploymentParametersResource input,
			@PathVariable("name") String name) {

		logger.info("Account " + principal.getName() + " configuration " + input.getName() + " addition requested");

		Account account = this.accountService.findByUsername(principal.getName());

		// Check configuration deployment parameters name isn't empty
		if (input.getName()==null || input.getName().length()<1) {
			throw new InvalidConfigurationDeploymentParametersException(account.getUsername(), input.getName());
		}

		// Create model entity
		ConfigurationDeploymentParameters deploymentParameters =
				this.configurationDeploymentParametersService.findByReference(input.getReference());

		ConfigDeploymentParamsCopy deploymentParametersCopy = null;
		if(input.getReference() != null){
			deploymentParametersCopy = this.configDeploymentParamsCopyService.
					findByConfigurationDeploymentParametersReference(input.getReference());
		}else{
			deploymentParametersCopy = this.configDeploymentParamsCopyService.
					findByNameAndAccountUsername(input.getName(), principal.getName());
		}

		deploymentParameters = this.configurationService.updateDeploymentParameterFields(deploymentParameters, deploymentParametersCopy, 
				input, configDeploymentParamsCopyService);

		// Prepare response
		HttpHeaders httpHeaders = new HttpHeaders();


		return new ResponseEntity<>(new ConfigurationDeploymentParametersResource(deploymentParameters), httpHeaders, HttpStatus.OK);

	}

	@RequestMapping(value = "/deploymentparameters/{name:.+}", method = { RequestMethod.DELETE } )
	public ResponseEntity<?> deleteDeploymentParameters(Principal principal, @PathVariable("name") String name) throws Exception 
	{

		logger.info("Account " + principal.getName() + " configuration " + name + " deletion requested");


		try{
			ConfigurationDeploymentParameters cdp = this.configurationDeploymentParametersService.findByNameAndAccountUserName(name, principal.getName());
			ConfigDeploymentParamsCopy cdpCopy = this.configDeploymentParamsCopyService.findByConfigurationDeploymentParametersReference(cdp.getReference());

			//stop all deployments using the configuration deployment parameters
			this.configurationService.stopDeploymentsOnDeletionOfDeploymentParameters(name,
					deploymentService, 
					deploymentRestController, 
					cdpCopy, 
					configDeploymentParamsCopyService);
			this.configurationDeploymentParametersService.delete(cdp);
			this.configDeploymentParamsCopyService.checkAndDeleteCDPCopy(cdpCopy, deploymentService, configurationService);
		}catch(ConfigurationDeploymentParametersNotFoundException e){
			logger.error("Configuration Deployment Parameters to delete is not found");
			throw e;
		}catch(ConfigDeploymentParamsCopyNotFoundException e){
			logger.error("Configuration Deployment Parameters copy not found");
			throw e;
		}


		// Prepare response
		HttpHeaders httpHeaders = new HttpHeaders();

		return new ResponseEntity<>(null, httpHeaders, HttpStatus.OK);
	}

	@RequestMapping(value = "/deploymentparameters/byconfiguration", method = {RequestMethod.POST, RequestMethod.OPTIONS} )
	public  ResponseEntity<?> getDeploymentParametersByConfiguration(Principal principal, 
			@RequestBody ConfigurationResource input) {

		logger.info("Deployment parameters of configuration '" + input.getName() + "' requested");

		Configuration configuration = configurationService.findByNameAndAccountUsername(input.getName(), input.getAccountUsername());



		logger.info("Getting deployment parameters for user configuration");
		ConfigurationDeploymentParameters configurationDeploymentParameters = this.configurationDeploymentParametersService
				.findByReference(configuration.getConfigDeployParamsReference());
		ConfigurationDeploymentParametersResource configurationDeploymentParametersResource =
				new ConfigurationDeploymentParametersResource(configurationDeploymentParameters);


		// Prepare response
		HttpHeaders httpHeaders = new HttpHeaders();

		return new ResponseEntity<>(configurationDeploymentParametersResource, httpHeaders, HttpStatus.CREATED);

	}

	@RequestMapping(value = "/shared", method = {RequestMethod.GET})
	public Resources<ConfigurationResource> getSharedConfigurationsByAccount(@RequestParam(name = "applicationReference"
			, required = false) String applicationReference, HttpServletRequest request, Principal principal) {
		logger.info("List of shared configurations of account requested " + principal.getName() + "  requested");
		logger.info("Filter based application,  reference :- " + applicationReference);
		Account account = this.accountService.findByUsername(principal.getName());
		String token = request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
		Set<Configuration> configurations = configurationService.getSharedConfigurationsByAccount(account, token, tokenHandler.parseUserFromToken(token));
		List<ConfigurationResource> configurationResourceList;
		//Filter configuration, send only configuration that is applicable if applicationReference is passed
		if (applicationReference != null && !applicationReference.equals("")) {
			configurations = filterConfigurationByApplication(configurations, applicationReference, account);
		}
		configurationResourceList = this.configurationService.createConfigurationResource(configurations);
		configurationResourceList = configurationService.checkObsoleteConfigurations(configurationResourceList, account, this.configDeploymentParamsCopyService);
		logger.info("Got shared user configurations  " + configurationResourceList.size());
		return new Resources<>(configurationResourceList);

	}

	@RequestMapping(value = "/shared/{name:.+}", method = {RequestMethod.GET})
	public ConfigurationResource getSharedByName(HttpServletRequest request, Principal principal, @PathVariable("name") String name) {

		logger.info("Account " + principal.getName() + " requested shared application " + name);

		Account account = this.accountService.findByUsername(principal.getName());
		String token = request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];

		try{
			Configuration configuration = configurationService.getSharedConfigurationByName(account, token, tokenHandler.parseUserFromToken(token), name);
			if(configuration != null){
				Configuration ownerConfiguration = configurationService.findByNameAndAccountUsername(name, configuration.getAccount().getUsername());
				CloudProviderParamsCopy cppCopy = cloudProviderParametersCopyService.findByCloudProviderParametersReference(configuration.getCloudProviderParametersReference());
				return new ConfigurationResource(ownerConfiguration, cppCopy);
			}else{
				throw new ConfigurationNotFoundException(name);
			}
		}catch(ConfigurationNotFoundException e) {
			throw new ConfigurationNotFoundException(name);
		}

	}

	@RequestMapping(value = "/shared/deploymentparameters", method = {RequestMethod.GET})
	public Resources<ConfigurationDeploymentParametersResource> getSharedConfigurationDeploymentParametersByAccount(
			HttpServletRequest request,
			Principal principal) {

		logger.info("List of shared configurations of account requested " + principal.getName() + "  requested");
		Account account = this.accountService.findByUsername(principal.getName());
		String token = request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
		List<ConfigurationDeploymentParametersResource> configurationDeploymentParametersResourceList = 
				this.configurationDeploymentParametersService.getSharedDeploymentParametersByAccount(account, token, tokenHandler.parseUserFromToken(token))
				.stream()
				.map(ConfigurationDeploymentParametersResource::new)
				.collect(Collectors.toList());

		return new Resources<>(configurationDeploymentParametersResourceList);

	}

	@RequestMapping(value = "/shared/deploymentparameters/{name:.+}", method = {RequestMethod.GET})
	public ConfigurationDeploymentParametersResource getSharedConfigurationDeploymentParametersByName(
			HttpServletRequest request,Principal principal, @PathVariable("name") String name) {

		logger.info("Account " + principal.getName() + " requested shared application " + name);

		Account account = this.accountService.findByUsername(principal.getName());
		String token = request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
		ConfigurationDeploymentParameters configurationDeploymentParameters = 
				this.configurationDeploymentParametersService.getSharedApplicationByDeploymentParametersName(account, 
						token, this.tokenHandler.parseUserFromToken(token), name);

		if (configurationDeploymentParameters!=null) {
			return new ConfigurationDeploymentParametersResource(
					this.configurationDeploymentParametersService.findByNameAndAccountUserName(name, configurationDeploymentParameters.getAccount().getUsername())
					);
		} else {
			throw new ConfigurationDeploymentParametersNotFoundException(name);
		}

	}

	@RequestMapping(value = "/ownedandshared/deploymentparameters", method = {RequestMethod.GET})
	public Resources<ConfigurationDeploymentParametersResource> getOwnedAndSharedDeploymentParameters(
			HttpServletRequest request, Principal principal) {

		logger.info("List of owned and shared deployment parameters  of account requested " + principal.getName() + "  requested");

		Account account = this.accountService.findByUsername(principal.getName());
		String token = request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];

		logger.info("--- Retrieving owned deployment parameters by username " + account.getUsername() + " for principal " + principal.getName());
		Collection<ConfigurationDeploymentParameters> deploymentParameters = 
				this.configurationDeploymentParametersService.findByAccountUsername(account.getUsername());
		List<ConfigurationDeploymentParametersResource> configurationDeploymentParametersResourceList = 
				deploymentParameters
				.stream()
				.map(ConfigurationDeploymentParametersResource::new)
				.collect(Collectors.toList());
		logger.info("Retrieved owned deployment parameters " + configurationDeploymentParametersResourceList.size());
		logger.info("--- Retrieving shared deployment parameters by username " + account.getUsername() + " for principal " + principal.getName());
		configurationDeploymentParametersResourceList.addAll(
				this.configurationDeploymentParametersService.getSharedDeploymentParametersByAccount(account, token, tokenHandler.parseUserFromToken(token))
				.stream()
				.map(ConfigurationDeploymentParametersResource::new)
				.collect(Collectors.toList()));
		logger.info("Retrieved owned and shared deployment parameters " + configurationDeploymentParametersResourceList.size());
		return new Resources<>(configurationDeploymentParametersResourceList);

	}

	@RequestMapping(value = "/{configurationReference}/deployments", method = RequestMethod.GET)
	public ResponseEntity<?> getAllConfigurationDeployments(
            Principal principal,
            @PathVariable("configurationReference") String configurationReference,
			@QueryParam("hideDestroyed") boolean hideDestroyed) {
		String userId = principal.getName();

		logger.info("Deployment list requested for configuration " + configurationReference);

		Account theAccount = this.accountService.findByUsername(userId);
		Configuration theConfiguration = this.configurationService.getByReference(
				theAccount.getUsername(),
				configurationReference);

		if (!theConfiguration.getAccount().getUsername().equals(theAccount.getUsername())) {
			return new ResponseEntity(HttpStatus.FORBIDDEN);
		}

		Collection<Deployment> configDeployments = this.deploymentService.findDeployments(userId, hideDestroyed)
				.stream().filter(deployment -> deployment.getDeploymentConfiguration().getConfigurationReference().equals(theConfiguration.getReference()))
				.collect(Collectors.toList());

		configDeployments.stream().forEach( d -> {
			logger.debug("There are " + d.getGeneratedOutputs().size() + " generated outputs for deployment " + d.getReference());
		});
		List<DeploymentResource> deploymentResourceList = new ArrayList<>();
		configDeployments.forEach(deployment -> {
			if(deployment.getCloudProviderParametersReference() != null){
				try{
					CloudProviderParamsCopy cppCopy = this.cloudProviderParametersCopyService.findByCloudProviderParametersReference(deployment.getCloudProviderParametersReference());
					deploymentResourceList.add(new DeploymentResource(deployment, cppCopy));
				}catch(CloudProviderParamsCopyNotFoundException e){
					logger.error("Could not find the cloud provider parameter copy ");
				}
			}else{
				deploymentResourceList.add(new DeploymentResource(deployment, null));
			}
		});

		return new ResponseEntity(deploymentResourceList, HttpStatus.OK);
	}

	@RequestMapping(value = "/{configurationReference:.+}/usage", method = { RequestMethod.GET } )
	public ResponseEntity<?> getConfigurationUsageByReference(Principal principal, @PathVariable("configurationReference") String configurationReference)  {
		//logger.info("Total usage requested for configuration " + configurationReference);

		String userId = principal.getName();

        DeploymentIndexService deploymentIndexService = new DeploymentIndexService(
                new RestTemplate(),
                this.elasticSearchUrl + "/" + this.elasticSearchIndex,
                this.elasticSearchUsername,
                this.elasticSearchPassword);

		double totalUsage = this.configurationService.getTotalConsumptionByReference(userId, configurationReference, deploymentIndexService);

		return new ResponseEntity(totalUsage, HttpStatus.OK);
	}
}
