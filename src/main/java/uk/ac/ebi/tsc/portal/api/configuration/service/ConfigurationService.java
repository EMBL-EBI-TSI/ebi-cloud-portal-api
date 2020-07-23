package uk.ac.ebi.tsc.portal.api.configuration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopy;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.*;
import uk.ac.ebi.tsc.portal.api.configuration.controller.ConfigurationDeploymentParametersResource;
import uk.ac.ebi.tsc.portal.api.configuration.controller.ConfigurationResource;
import uk.ac.ebi.tsc.portal.api.configuration.controller.InvalidConfigurationInputException;
import uk.ac.ebi.tsc.portal.api.configuration.repo.*;
import uk.ac.ebi.tsc.portal.api.deployment.controller.DeploymentRestController;
import uk.ac.ebi.tsc.portal.api.deployment.repo.Deployment;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentStatusEnum;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;
import uk.ac.ebi.tsc.portal.api.utils.SendMail;
import uk.ac.ebi.tsc.portal.usage.deployment.model.DeploymentDocument;
import uk.ac.ebi.tsc.portal.usage.deployment.service.DeploymentIndexService;

import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Service
public class ConfigurationService {

	private final ConfigurationRepository configurationRepository;
	private final DomainService domainService;
	private final CloudProviderParametersService cppService;
	private final ConfigurationDeploymentParametersService cdpService;
	private final CloudProviderParamsCopyService cloudProviderParametersCopyService;
	private final DeploymentService deploymentService;
	private final SendMail sendMail;

	private static final Logger logger = LoggerFactory.getLogger(ConfigurationDeploymentParametersService.class);

	@Autowired
	public ConfigurationService(ConfigurationRepository configurationRepository, DomainService domainService,
			CloudProviderParametersService cppService, ConfigurationDeploymentParametersService cdpService,
			CloudProviderParamsCopyService cloudProviderParametersCopyService, DeploymentService deploymentService,
			SendMail sendMail) {
		this.configurationRepository = configurationRepository;
		this.domainService = domainService;
		this.cppService = cppService;
		this.cdpService = cdpService;
		this.cloudProviderParametersCopyService = cloudProviderParametersCopyService;
		this.deploymentService = deploymentService;
		this.sendMail = sendMail;
	}

	public Collection<Configuration> findByAccountUsername(String username) {
		return this.configurationRepository.findByAccountUsername(username);
	}

	public Configuration save(Configuration configuration) {
		return this.configurationRepository.save(configuration);
	}

	public void deleteByNameAndAccountName(String name, String username) {
		Configuration configuration = this.configurationRepository.findByNameAndAccountUsername(name, username).orElseThrow(
				() -> new ConfigurationNotFoundException(name, username));
		this.configurationRepository.delete(configuration.getId());
	}

	public Configuration findByNameAndAccountUsername(String name, String username) {
		return this.configurationRepository.findByNameAndAccountUsername(name, username).orElseThrow(
				() -> new ConfigurationNotFoundException(username, name)
				);
	}

	public Configuration findByNameAndAccountEmail(String name, String email){
		return this.configurationRepository.findByNameAndAccountEmail(name, email).orElseThrow(
				() -> new ConfigurationNotFoundException(name)
				);
	}

	public Configuration findByReference(String reference){
		return this.configurationRepository.findByReference(reference).orElseThrow(
				() -> new ConfigurationNotFoundException(reference)
				);
	}

	public List<Configuration> findAll(){
		return this.configurationRepository.findAll();
	}

	public void delete(Configuration configuration){
		this.configurationRepository.delete(configuration);
	}

	public Set<Configuration> getSharedConfigurationsByAccount(Account account, String token,
			User user) {

		Set<Configuration> sharedConfigurations = new HashSet<>();
		logger.info("In ConfigurationService: getting shared configurations");

		for(Team memberTeam: account.getMemberOfTeams()){
			try{
				logger.info("In ConfigurationService: checking if team has a domain reference ");
				if(memberTeam.getDomainReference() != null){
					logger.info("In ConfigurationService: check if user is a domain member");
					Domain domain = domainService.getDomainByReference(memberTeam.getDomainReference(), token);
					if(domain != null){
						Collection<User> users = domainService.getAllUsersFromDomain(domain.getDomainReference(), token);
						User domainUser = users.stream().filter(u -> u.getEmail().equals(user.getEmail())).findAny().orElse(null);
						if(domainUser != null){
							logger.info("In ConfigurationService:  returning shared configurations , if user is member of team and domain");
							try{
								sharedConfigurations.addAll(memberTeam.getConfigurationsBelongingToTeam());
							}catch(Exception e){
								logger.error("In ConfigurationService: Failed to get team configurations, for team with domain reference " + memberTeam.getName());
							}
						}
					}
				}else{
					logger.info("In ConfigurationService:  returning shared configuration , if user is member of team and no domain present");
					try{
						sharedConfigurations.addAll(memberTeam.getConfigurationsBelongingToTeam());
					}catch(Exception e){
						logger.error("In ConfigurationService: Failed to get team configurations, for team with no domain configurations " + memberTeam.getName());
					}
				}
			}catch(Exception e){
				logger.error("In ConfigurationService: Could not add all shared configurations from team " + memberTeam.getName());
			}
		}
		return sharedConfigurations;
	}

	public Configuration getSharedConfigurationByName(Account account,  String token, User user, String configurationName ){

		logger.info("In ConfigurationService:  getting shared configuration  by name");
		for(Team memberOfTeam: account.getMemberOfTeams()){
			try{
				logger.info("In ConfigurationService: checking if team has a domain reference ");
				if(memberOfTeam.getDomainReference() != null){
					logger.info("In ConfigurationService:  check if user is a domain member");
					Domain domain = domainService.getDomainByReference(memberOfTeam.getDomainReference(), token);
					if(domain != null){
						Collection<User> users = domainService.getAllUsersFromDomain(domain.getDomainReference(), token);
						User domainUser = users.stream().filter(u -> u.getEmail().equals(user.getEmail())).findAny().orElse(null);
						if(domainUser != null){
							logger.info("In ConfigurationService: returning the shared configuration, if user is member of team and domain");
							return memberOfTeam.getConfigurationsBelongingToTeam().stream().filter(c -> c.getName().equals(configurationName))
									.findFirst().get();
						}
					}
				}else{
					logger.info("In ConfigurationService:  returning shared configuration by name , if user is member of team and no domain present");
					return memberOfTeam.getConfigurationsBelongingToTeam().stream().filter(c -> c.getName().equals(configurationName))
							.findFirst().get();
				}
			}catch(Exception e){
				logger.error("In ConfigurationService: Could not add shared shared configuration " + configurationName + e.getMessage() );
			}
		}
		return null;
	}

	public List<ConfigurationResource> checkObsoleteConfigurations(
			List<ConfigurationResource> configurationResourceList, 
			Account account, ConfigDeploymentParamsCopyService configDeploymentParamsCopyService) {

		logger.info("Checking obsolete configurations");
		//check if configuration is still usable by user
		configurationResourceList.forEach(configuration -> {
			try{
				logger.info("Checking configuration " + configuration.getReference() != null ? configuration.getReference() : null);
				logger.info("CPP copy reference " + configuration.getCloudProviderParametersReference() != null ? configuration.getCloudProviderParametersReference() : null);
				logger.info("CDP copy reference " +  configuration.getConfigDeploymentParametersReference()!= null ? configuration.getConfigDeploymentParametersReference() : null );

				//default deny
				configuration.setObsolete(true);
				CloudProviderParamsCopy cpp = null;
				boolean cppCanBeUsed = false;
				if (configuration.getCloudProviderParametersReference() != null) {
					try {
						cpp = cloudProviderParametersCopyService.findByCloudProviderParametersReference(configuration.getCloudProviderParametersReference());
						cppCanBeUsed = findIfCppCanBeUsed(cpp, account);
					} catch (CloudProviderParamsCopyNotFoundException e) {
						logger.info("Could not find the associated cloud provider copy for configuration " + configuration.getName());
					}

				}

				ConfigDeploymentParamsCopy cdp = null;
				boolean cdpCanBeUsed = false;
				if (configuration.getConfigDeploymentParametersReference() != null) {
					try {
						cdp = configDeploymentParamsCopyService.
								findByConfigurationDeploymentParametersReference(configuration.getConfigDeploymentParametersReference());
						cdpCanBeUsed = findIfCdpCanBeUsed(cdp, account, configuration);
					} catch (ConfigDeploymentParamsCopyNotFoundException e) {
						logger.info("Could not find the associated cloud provider params copy for configuration " + configuration.getName());
					}
				}

				//only if both cloud provider and deployment params exist, configuration is usable
				if (cppCanBeUsed && cdpCanBeUsed) {
					configuration.setObsolete(false);
				}

			}catch(ConfigurationNotFoundException e){
				logger.error("Configuration with reference " + configuration.getReference() +
						" could not be found");
			}
		});
		logger.info("Returning all configuration");
		return configurationResourceList;
	}

	private boolean findIfCppCanBeUsed(CloudProviderParamsCopy cppCopy, Account account) {

		try{
			logger.info("Checking if the account holder owns the cloud credential");
			CloudProviderParameters cpp = this.cppService.findByReference(cppCopy.getCloudProviderParametersReference());
			if(cpp.getAccount().equals(account)){
				return true;
			}else{
				logger.info("The user does not own the credential, checking if the account holder belongs to the team, cloud credential is shared with");
				Team memberTeam = account.getMemberOfTeams().stream().filter(team -> cpp.getSharedWithTeams().contains(team)).findAny().orElse(null);
				if (memberTeam != null){
					logger.info("The account holder either owns or the cloud credential shared with him");
					return true;
				}else{
					logger.info("The account holder neither owns nor is the cloud credential is shared with him");
					return false;
				}
			}
		}catch(CloudProviderParametersNotFoundException e){
			//cloud credential has been deleted so obsolete configuration
			return false;
		}
	}

	private boolean findIfCdpCanBeUsed(ConfigDeploymentParamsCopy cdpCopy, Account account, ConfigurationResource configurationResource) {

		try{
			logger.info("Checking if the account holder owns the deployment parameter.");
			ConfigurationDeploymentParameters cdp = this.cdpService.findByReference(cdpCopy.getConfigurationDeploymentParametersReference());
			if(cdp.getAccount().equals(account)){
				return true;
			}else{
				logger.info("The user does not own the deployment parameter, checking if the account holder belongs to the team, it is shared with.");
				Team memberTeam = account.getMemberOfTeams().stream().filter(team -> cdp.getSharedWithTeams().contains(team)).findAny().orElse(null);
				if (memberTeam != null){
					return true;
				}else{
					logger.info("The user does not own the deployment parameter and it is not shared with him, "
							+ "but check if the configuation is shared with him.");
					logger.info("Find the configuration with reference " + configurationResource.getReference());
					Configuration configuration = this.findByReference(configurationResource.getReference());
					Team memberTeamOfConfiguration = account.getMemberOfTeams().stream().filter(team -> configuration.getSharedWithTeams().contains(team)).findAny().orElse(null);
					if(memberTeamOfConfiguration != null){
						logger.info("The account holder either owns or the deployment parameter is shared is with him");
						return true;
					}else{
						logger.info("The account holder neither owns nor is the deployment parameter shared with him");
						return false;
					}
				}
			}
		}catch(ConfigurationDeploymentParametersNotFoundException e){
			logger.info("The deployment parameters could not be found.");
			return false;
		}
	}

	public Configuration update(Account account, Principal principal, ConfigurationResource input, 
			String name, ConfigDeploymentParamsCopyService configDeploymentParamsCopyService) {

		// Check configuration name isn't empty
		if (input.getName()==null || input.getName().length()<1) {
			throw new InvalidConfigurationInputException(account.getUsername(), input.getName());
		}

		Configuration configuration = this.findByNameAndAccountUsername(name, principal.getName());

		//Updating cloud provider id requested
		if(!configuration.getCloudProviderParametersName().equals(input.getCloudProviderParametersName())){

			logger.info("Cloud Provider has been requested to be updated");

			//updating cloud provider if requested
			CloudProviderParameters cloudProviderParameters = null;
			try{
				logger.info("Cloud Provider has been requested to be updated, check if user owns the credential");
				cloudProviderParameters = this.cppService.findByNameAndAccountUsername(
						input.getCloudProviderParametersName(), principal.getName());
			}catch(CloudProviderParametersNotFoundException e){
				logger.info("Cloud Provider has been requested to be updated, check if user has been shared the credential");
				cloudProviderParameters = this.cppService.findByName(input.getCloudProviderParametersName());
				Set<Team> sharedWithTeams = cloudProviderParameters.getSharedWithTeams();
				if(account.getMemberOfTeams().stream().filter(t -> sharedWithTeams.contains(t)).collect(Collectors.toList()).isEmpty()){
					cloudProviderParameters = null;
				}
			}

			if (cloudProviderParameters == null) {
				logger.info("User has no access to the cloud provider, choosen, so cannot be updated");
				throw new CloudProviderParametersNotFoundException(account.getUsername());
			}else{
				logger.info("Updating configuration, with the new credential");
				configuration.setCloudProviderParametersReference(cloudProviderParameters.getReference());
				configuration.setCloudProviderParametersName(cloudProviderParameters.getName());
			}

		}

		//Updating sshkey if requested
		if(!configuration.getSshKey().equals(input.getSshKey())){
			logger.info("Updating ssh key with new value, as requested by user");
			configuration.setSshKey(input.getSshKey());
		}

		//Updating limits if requested
		if((input.getSoftUsageLimit()!=null) && !((configuration.getSoftUsageLimit()!=null) && configuration.getSoftUsageLimit().equals(input.getSoftUsageLimit()))) {
			logger.info("Updating soft usage limits with new value, as requested by user");
			configuration.setSoftUsageLimit(input.getSoftUsageLimit());
		} else if (input.getSoftUsageLimit()==null) {
			logger.info("Resetting soft usage limits, as requested by user");
			configuration.setSoftUsageLimit(null);
		}
		if((input.getHardUsageLimit()!=null) && !((configuration.getHardUsageLimit()!=null) && configuration.getHardUsageLimit().equals(input.getHardUsageLimit()))) {
			logger.info("Updating hard usage limits with new value, as requested by user");
			configuration.setHardUsageLimit(input.getHardUsageLimit());
		} else if (input.getHardUsageLimit()==null) {
			logger.info("Resetting hard usage limits, as requested by user");
			configuration.setHardUsageLimit(null);
		}
		//updating deployment parameters if requested
		ConfigDeploymentParamsCopy configDeploymentParamsCopy = configDeploymentParamsCopyService.findByConfigurationDeploymentParametersReference(configuration.getConfigDeployParamsReference());
		if(!configDeploymentParamsCopy.getName().equals(input.getDeploymentParametersName())){

			logger.info("User has requested, deployment parameters to be updated");

			//get the deployment parameters 	
			ConfigDeploymentParamsCopy  deploymentParametersCopy = 
					configDeploymentParamsCopyService.findByName(input.getDeploymentParametersName());
			configuration.setConfigDeployParamsReference(deploymentParametersCopy.getConfigurationDeploymentParametersReference());
		}

		// Persist
		configuration = this.save(configuration);
		return configuration;

	}

	public ConfigurationDeploymentParameters updateDeploymentParameterFields(
			ConfigurationDeploymentParameters deploymentParameters, ConfigDeploymentParamsCopy deploymentParametersCopy, 
			ConfigurationDeploymentParametersResource input, ConfigDeploymentParamsCopyService configDeploymentParamsCopyService) {

		//update changed values
		logger.info("In ConfigurationService: checking to see if deployment parameters are to be updated");
		deploymentParameters.getConfigurationDeploymentParameter().forEach(field -> {
			input.getFields().stream().filter(f -> f.getKey().equals(field.getKey()) && !f.getValue().equals(field.getValue()))
			.forEach(matchedField -> {
				logger.info("In ConfigurationService: updated value found for " + matchedField.getKey());
				field.setValue(matchedField.getValue());
			});
		});

		if(deploymentParametersCopy != null){
			//update changed values in copy
			logger.info("In ConfigurationService: checking to see if deployment parameters copy are to be updated");
			deploymentParametersCopy.getConfigDeploymentParamCopy().forEach(field -> {
				input.getFields().stream().filter(f -> f.getKey().equals(field.getKey()) && !f.getValue().equals(field.getValue()))
				.forEach(matchedField -> {
					logger.info("In ConfigurationService: updated value found for " + matchedField.getKey());
					field.setValue(matchedField.getValue());
				});
			});
		}


		//remove unused 
		List<String> parameterFieldKeysPresent = deploymentParameters.getConfigurationDeploymentParameter().stream().map(f -> f.getKey()).collect(Collectors.toList());
		List<String> inputFieldKeysPresent = input.getFields().stream().map(f -> f.getKey()).collect(Collectors.toList());
		//if the input field keys does not contain key, mark it for removal
		List<String> parameterFieldKeysToRemove = parameterFieldKeysPresent.stream().filter(f -> !inputFieldKeysPresent.contains(f)).collect(Collectors.toList());
		//Get list of fields 
		List<ConfigurationDeploymentParameter> cdpFieldsToRemove = new ArrayList<>();
		if(!parameterFieldKeysToRemove.isEmpty()){
			logger.info("In ConfigurationService: removing deleted parameter");
			deploymentParameters.getConfigurationDeploymentParameter().forEach(field -> {
				if(parameterFieldKeysToRemove.contains(field.getKey())){
					logger.info("In ConfigurationService: removing deleted parameter " + field.getKey());
					cdpFieldsToRemove.add(field);
				}
			});
		}
		deploymentParameters.getConfigurationDeploymentParameter().removeAll(cdpFieldsToRemove);

		//remove unused in copy
		if(deploymentParametersCopy != null){
			List<String> parameterFieldKeysPresentCopy = deploymentParametersCopy.getConfigDeploymentParamCopy().stream().map(f -> f.getKey()).collect(Collectors.toList());
			List<String> inputFieldKeysPresentCopy = input.getFields().stream().map(f -> f.getKey()).collect(Collectors.toList());
			//if the input field keys does not contain key, mark it for removal
			List<String> parameterFieldKeysToRemoveCopy = parameterFieldKeysPresentCopy.stream().filter(f -> !inputFieldKeysPresentCopy.contains(f)).collect(Collectors.toList());
			//Get list of fields 
			List<ConfigDeploymentParamCopy> cdpCopyFieldsToRemove = new ArrayList<>();
			if(!parameterFieldKeysToRemoveCopy.isEmpty()){
				logger.info("In ConfigurationService: removing deleted parameter copy");
				deploymentParametersCopy.getConfigDeploymentParamCopy().forEach(field -> {
					if(parameterFieldKeysToRemoveCopy.contains(field.getKey())){
						logger.info("In ConfigurationService: removing deleted parameter copy " + field.getKey());
						cdpCopyFieldsToRemove.add(field);
					}
				});
			}
			deploymentParametersCopy.getConfigDeploymentParamCopy().removeAll(cdpCopyFieldsToRemove);
		}


		//add new
		logger.info("In ConfigurationService:: checking to see if new parameters are to be added");
		List<ConfigurationDeploymentParameter> inputFields = input.getFields().stream().map(f -> new ConfigurationDeploymentParameter(f.getKey(), f.getValue(), deploymentParameters)).collect(Collectors.toList());
		List<String> presentFields = deploymentParameters.getConfigurationDeploymentParameter().stream().map(f -> f.getKey()).collect(Collectors.toList());
		List<ConfigurationDeploymentParameter> parameterFieldKeysToAdd = inputFields.stream().filter(f -> !presentFields.contains(f.getKey())).collect(Collectors.toList());
		if(!parameterFieldKeysToAdd.isEmpty()){
			logger.info("In ConfigurationService:: adding new " + parameterFieldKeysToAdd.size() + " parameter(s)");
			deploymentParameters.getConfigurationDeploymentParameter().addAll(parameterFieldKeysToAdd);
		}

		//add new copy
		if(deploymentParametersCopy != null){
			logger.info("In ConfigurationService:: checking to see if new parameters are to be added in copy");
			List<ConfigDeploymentParamCopy> inputFieldsCopy = input.getFields().stream().map(f -> new ConfigDeploymentParamCopy(f.getKey(), f.getValue(), deploymentParametersCopy)).collect(Collectors.toList());
			List<String> presentFieldsCopy = deploymentParametersCopy.getConfigDeploymentParamCopy().stream().map(f -> f.getKey()).collect(Collectors.toList());
			List<ConfigDeploymentParamCopy> parameterFieldKeysToAddCopy = inputFieldsCopy.stream().filter(f -> !presentFieldsCopy.contains(f.getKey())).collect(Collectors.toList());
			if(!parameterFieldKeysToAddCopy.isEmpty()){
				logger.info("In ConfigurationService:: adding new " + parameterFieldKeysToAddCopy.size() + " parameter(s) copy");
				deploymentParametersCopy.getConfigDeploymentParamCopy().addAll(parameterFieldKeysToAddCopy);
			}
			configDeploymentParamsCopyService.save(deploymentParametersCopy);
		}
		return cdpService.save(deploymentParameters);
	}

	public void stopDeploymentsOnDeletionOfConfiguration(String name, 
			Principal principal,
			DeploymentService deploymentService,
			DeploymentRestController deploymentRestController,
			Configuration configuration){

		logger.info("Stop all deployments using the configuration " + configuration.getName());
		List<String> toNotify = new ArrayList<>();
		List<Deployment> deployments =  new ArrayList<>();
		deploymentService.findAll().stream().forEach(d -> {
			if(d.getDeploymentConfiguration() != null){
				if(d.getDeploymentConfiguration().getConfigurationReference() != null){
					logger.info("Deployment configuration reference is " + d.getDeploymentConfiguration().getConfigurationReference());
					deployments.add(d);
				}
			}
		});

		deployments.forEach(deployment -> {
			if(deployment.getDeploymentConfiguration().getConfigurationReference().equals(configuration.getReference())){
				try{
					if(deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.RUNNING)
							|| deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.STARTING)){
						logger.info("Found deployment using the configuration, stopping it");
						deploymentRestController.stopByReference(deployment.getReference());
						toNotify.add(deployment.getAccount().getEmail());
					}
				}catch(Exception e){
					logger.info("Failed to stop deployments on deleting configuration");
				}
			}
		});

		if(!toNotify.isEmpty()){
			logger.info("There are users who are to be notified, regarding deployments destruction");
			String message = "Your deployments were destroyed. \n"
					+ "This was because the configuration " + "'" + configuration.name + "'" +" was \n"
					+ " deleted by its owner " + configuration.getAccount().givenName + ".";
			try{
				sendMail.send(toNotify, "Deployments destroyed", message );
			}catch(IOException e){
				logger.error("Failed to send, deployments destroyed notification on deleting "
						+ "configuration'" + configuration.getName() + "' ." );

			}
		}
	}


	public ConfigurationDeploymentParameters createNewDeploymentParameters(Principal principal, Account account,
			ConfigurationDeploymentParametersResource input, ConfigurationDeploymentParameterService configurationDeploymentParameterService,
			ConfigDeploymentParamsCopyService configDeploymentParamsCopyService,
			ConfigDeploymentParamCopyService configDeploymentParamCopyService) {

		// Create model entity
		ConfigurationDeploymentParameters deploymentParameters = 
				new ConfigurationDeploymentParameters(input.getName(), account);
		deploymentParameters.setReference(UUID.randomUUID().toString());

		// Add fields
		deploymentParameters.getConfigurationDeploymentParameter().addAll(input.getFields().stream().map(
				f -> new ConfigurationDeploymentParameter(f.getKey(),f.getValue(), deploymentParameters)
				).collect(Collectors.toList()));

		// Persist
		ConfigurationDeploymentParameters savedConfigurationDeploymentParameters = this.cdpService.save(deploymentParameters);
		if(savedConfigurationDeploymentParameters!= null)
			savedConfigurationDeploymentParameters.getConfigurationDeploymentParameter().forEach(parameter
					-> configurationDeploymentParameterService.save(parameter));

		//create and persist copy
		ConfigDeploymentParamsCopy configDeploymentParamsCopy = new ConfigDeploymentParamsCopy(savedConfigurationDeploymentParameters);
		configDeploymentParamsCopy.getConfigDeploymentParamCopy().addAll(
				savedConfigurationDeploymentParameters.getConfigurationDeploymentParameter()
				.stream().map(f-> new ConfigDeploymentParamCopy(f.getKey(), f.getValue(), configDeploymentParamsCopy))
				.collect(Collectors.toList())
				);
		ConfigDeploymentParamsCopy savedConfigDeploymentParamsCopy = configDeploymentParamsCopyService.save(configDeploymentParamsCopy);
		if(savedConfigDeploymentParamsCopy != null){
			savedConfigDeploymentParamsCopy.getConfigDeploymentParamCopy().forEach(
					parameter -> configDeploymentParamCopyService.save(parameter)
					);
		}

		return deploymentParameters;
	}

	public void deleteCPPAndCDPCopies(String cppReference, String cdpsReference,
			DeploymentService deploymentService, ConfigDeploymentParamsCopyService configDeploymentParamsCopyService) {
		try{
			logger.info("Checking if the cloud provider parameters exists");
			cppService.findByReference(cppReference);
		}catch(CloudProviderParametersNotFoundException e){
			logger.info("Cloud provider parameter does not exist, check and delete the cloud provider parameters copy if it is not referenced");
			try{
				CloudProviderParamsCopy cppCopy = this.cloudProviderParametersCopyService.findByCloudProviderParametersReference(cppReference);
				this.cloudProviderParametersCopyService.checkAndDeleteCPPCopy(cppCopy, deploymentService, this);
			}catch(CloudProviderParamsCopyNotFoundException ex){
				logger.error("Could not find the cloud provider parameter copy with reference " + cppReference );
			}
		}

		try{
			logger.info("Checking if the config deployment params exists");
			if(cdpsReference != null){
				cdpService.findByReference(cdpsReference);
			}
		}catch(ConfigurationDeploymentParametersNotFoundException e){
			logger.info("Config deployment params does not exist, check and delete the config deployment params copy if it is not referenced");
			try{
				ConfigDeploymentParamsCopy cdpCopy = configDeploymentParamsCopyService.findByConfigurationDeploymentParametersReference(cdpsReference);
				configDeploymentParamsCopyService.checkAndDeleteCDPCopy(cdpCopy, deploymentService, this);
			}catch(ConfigDeploymentParamsCopyNotFoundException ex){
				logger.error("Could not find the deployment parameters copy with reference " + cdpsReference );
			}
		}

	}

	public void stopDeploymentsOnDeletionOfDeploymentParameters(String name, 
			DeploymentService deploymentService, 
			DeploymentRestController deploymentRestController,
			ConfigDeploymentParamsCopy cdpCopy,
			ConfigDeploymentParamsCopyService cdpCopyService) {

		logger.info("Stop all deployments using the configuration deployment parameter" + cdpCopy.getName());
		List<String> toNotify = new ArrayList<>();
		List<Deployment> deployments =  new ArrayList<>();
		deploymentService.findAll().stream().forEach(d -> {
			if(d.getDeploymentConfiguration() != null){
				if(d.getDeploymentConfiguration().getConfigDeploymentParametersReference() != null){
					logger.info("Deployment configuration parameters reference is " + 
							d.getDeploymentConfiguration().getConfigDeploymentParametersReference());
					deployments.add(d);
				}
			}
		});

		deployments.forEach(deployment -> {
			try{
				//additional check
				ConfigDeploymentParamsCopy cdpCopyFound = cdpCopyService.
						findByConfigurationDeploymentParametersReference(deployment.getDeploymentConfiguration().getConfigDeploymentParametersReference());
				if(cdpCopyFound.getConfigurationDeploymentParametersReference().equals(cdpCopy.getConfigurationDeploymentParametersReference())){
					try{
						if(deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.RUNNING)
								|| deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.STARTING)){
							logger.info("Found deployment using the configuration deployment parameter, stopping it");
							deploymentRestController.stopByReference(deployment.getReference());
							toNotify.add(deployment.getAccount().getEmail());
						}
					}catch(Exception e){
						e.printStackTrace();
						logger.error("Failed to stop deployment, on deleting configuration deployment parameter");
					}
				}
			}catch(ConfigDeploymentParamsCopyNotFoundException e){
				e.printStackTrace();
				if(deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.RUNNING)
						|| deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.STARTING)){
					logger.error("Failed to stop deployments on deletion of deployment parameter,"
							+ "\n because the cdp copy was not found for deployment." + deployment.getReference());
				}else{
					logger.error("The cdp copy was not found for deployment, but no active deployment found for " + deployment.getReference());
				}
			}
		});

		if(!toNotify.isEmpty()){
			logger.info("There are users who are to be notified, regarding deployments destruction");
			String message = "Your deployments were destroyed. \n"
					+ "This was because the configuration deployment parameter " + "'" + cdpCopy.name + "'" +" was \n"
					+ " deleted by its owner " + cdpCopy.getAccount().givenName + ".";
			try{
				sendMail.send(toNotify, "Deployments destroyed", message );
			}catch(IOException e){
				logger.error("Failed to send, deployments destroyed notification on deleting "
						+ " configuration deployment parameter '" + cdpCopy.getName() + "' ." );
			}
		}

	}

	public List<ConfigurationResource> createConfigurationResource(Collection<Configuration> configurations) {
		logger.info("Creating a configuration resource");
		List<ConfigurationResource> configurationResources = new ArrayList<>();
		configurations.forEach(configuration -> {
			try{
				configurationResources.add(new ConfigurationResource(
						configuration,
						this.cloudProviderParametersCopyService.findByCloudProviderParametersReference(configuration.getCloudProviderParametersReference())));
			}catch(CloudProviderParamsCopyNotFoundException e){
				logger.info("Could not find the cloud provider copy for configuration " + configuration.getName());
				configurationResources.add(new ConfigurationResource(configuration, null));
			}
		});

		return configurationResources;
	}

	public Configuration getByReference(String username, String reference) {
		return this.configurationRepository.findByReference(reference).orElseThrow(
				() -> new ConfigurationNotFoundException(username, reference)
				);
	}

	public double getTotalConsumptionByReference(String username, String reference, DeploymentIndexService deploymentIndexService) {
		logger.debug("Calculating total usage for configuration " + reference);

		Configuration theConfiguration = this.getByReference(username, reference);

		return this.getTotalConsumption(theConfiguration, deploymentIndexService);
	}

	public double getTotalConsumption(Configuration configuration, DeploymentIndexService deploymentIndexService) {

		logger.debug("Calculating total usage for configuration " + configuration.getName());

		Configuration theConfiguration = configuration;

		logger.debug("Found configuration " + theConfiguration.getName());

		Collection<Deployment> theDeployments = this.deploymentService.findByConfigurationReference(theConfiguration.getReference());

		logger.debug("Found " + theDeployments.size() + " associated deployments");


		return theDeployments.stream().filter(d -> d.getDeployedTime()!=null).mapToDouble(d -> {
			logger.debug("Processing deployment " + d.getReference());
			double totalConsumption = 0.0;
			DeploymentDocument theDeploymentDocument = deploymentIndexService.findById(d.getReference());
			if (theDeploymentDocument != null) {
				logger.debug("Deployment found in index");
				double totalSeconds = theDeploymentDocument.getTotalRunningTime() * 0.001;
				double consumptionRatePerHour = 0.25 * (theDeploymentDocument.getTotalVcpus() + 0.5*theDeploymentDocument.getTotalRamGb());
				totalConsumption = (totalSeconds/3600.0) * consumptionRatePerHour;

			} else {
				logger.debug("Deployment NOT found in index");
			}
			return totalConsumption;
		}).sum();
	}

	public boolean isConfigurationSharedWithAccount(Account account, Configuration configuration){
		
		//get the list of teams with which config is shared, if the user is a member of any of those teams
		logger.info("Looking for shared configuration " + configuration.getName()	+ " belonging to " + account.getGivenName());
		if(account.getMemberOfTeams().stream().anyMatch(team ->
		configuration.getSharedWithTeams().stream().anyMatch(t -> (t.getDomainReference() != null) && (team.getDomainReference() != null) && t.getDomainReference().equals(team.getDomainReference())))){
			return true;
		}else{
			return false;
		}
	}

	public boolean canConfigurationBeUsedForApplication(Configuration configuration, Application application, Account account) {
		//Check configuration is owned by user
		logger.info("Checking if configuration '" + configuration.getName() +
				"' can be used for application '" + application.getName() + "'" +
				"by user '" + account.getGivenName() + "'"
				);
		try{
			CloudProviderParameters cloudProviderParameters = cppService.findByReference(configuration.getCloudProviderParametersReference());
			if (configuration.getAccount().equals(account)) {
				//Checking credentials is can be used on any application
				logger.debug("The user is the configuration owner, so check if cloud credential is usable ");
				return cppService.canCredentialBeUsedForApplication(cloudProviderParameters, application, account);
			} else {
				//Check configuration is shared with user
				if (isConfigurationSharedWithAccount(account, configuration)) {
					//Getting corresponding teams
					Set<String> configSharedWithTeams = configuration.getSharedTeamNames();
					Set<String> accountMemberOfTeams = account.getMembershipTeamNames();
					Set<String> appSharedWithTeams = application.getSharedTeamNames();
					return cppService.checkForOverlapingAmongTeams(configSharedWithTeams,appSharedWithTeams, accountMemberOfTeams);
				}
			}
		}catch(CloudProviderParametersNotFoundException e){
			logger.error("The associated cloud provider parameter is not found, it is obsolete");
			return false;
		}

		return false;
	}


}
