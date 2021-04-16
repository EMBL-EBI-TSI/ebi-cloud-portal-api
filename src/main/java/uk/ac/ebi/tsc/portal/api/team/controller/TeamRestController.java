package uk.ac.ebi.tsc.portal.api.team.controller;

import org.apache.http.MethodNotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.service.AccountService;
import uk.ac.ebi.tsc.portal.api.application.controller.ApplicationResource;
import uk.ac.ebi.tsc.portal.api.application.controller.ApplicationRestController;
import uk.ac.ebi.tsc.portal.api.application.controller.InvalidApplicationInputException;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.application.service.ApplicationNotFoundException;
import uk.ac.ebi.tsc.portal.api.application.service.ApplicationService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.controller.CloudProviderParametersResource;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.controller.CloudProviderParametersRestController;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.controller.InvalidCloudProviderParametersInputException;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParametersNotFoundException;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParametersService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyService;
import uk.ac.ebi.tsc.portal.api.configuration.controller.ConfigurationDeploymentParametersResource;
import uk.ac.ebi.tsc.portal.api.configuration.controller.ConfigurationResource;
import uk.ac.ebi.tsc.portal.api.configuration.controller.InvalidConfigurationDeploymentParametersException;
import uk.ac.ebi.tsc.portal.api.configuration.controller.InvalidConfigurationInputException;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationDeploymentParametersNotFoundException;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationDeploymentParametersService;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationNotFoundException;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationService;
import uk.ac.ebi.tsc.portal.api.deployment.controller.DeploymentRestController;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentConfigurationService;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.encryptdecrypt.security.EncryptionService;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;
import uk.ac.ebi.tsc.portal.api.team.service.*;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDeployer;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.login.AccountNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@RestController
@RequestMapping(value = "/team", produces = {MediaType.APPLICATION_JSON_VALUE})
public class TeamRestController {

	private static final Logger logger = LoggerFactory.getLogger(TeamRestController.class);

	private final TeamService teamService;

	private final AccountService accountService;

	private final ApplicationService applicationService;

	// TODO
	// By @jadianes: all these controllers and controller interactions should go into the service layers, otherwise
	// we are coupling the controllers together and that's not good practice

	private final CloudProviderParametersService cloudProviderParametersService;

	private final ConfigurationDeploymentParametersService configDepParamsService;

	private final ConfigurationService configurationService;

	private final DeploymentService deploymentService;

	private final DeploymentConfigurationService deploymentConfigurationService;

	private final CloudProviderParamsCopyService cloudProviderParametersCopyService;

	private final DomainService domainService;

	@Autowired
	public TeamRestController(TeamService teamService,
                              AccountService  accountService,
                              ApplicationRestController applicationRestController,
                              CloudProviderParametersRestController cloudProviderParametersRestController,
                              ApplicationService applicationService, CloudProviderParametersService cppService,
                              ConfigurationDeploymentParametersService configDepParamsService,
                              ConfigurationService configService, DomainService domainService,
                              uk.ac.ebi.tsc.aap.client.security.TokenHandler tokenHandler,
                              DeploymentConfigurationService deploymentConfigurationService,
                              DeploymentService deploymentService,
                              DeploymentRestController deploymentRestController,
                              CloudProviderParamsCopyService cloudProviderParametersCopyService,
                              EncryptionService encryptionService,
                              ApplicationDeployer applicationDeployer,
                              CloudProviderParametersService cloudProviderParametersService,
                              ConfigurationService configurationService){
		this.cloudProviderParametersCopyService = cloudProviderParametersCopyService;
		this.accountService = accountService;
		this.applicationService = applicationService;
		this.cloudProviderParametersService = cloudProviderParametersService;
		this.configDepParamsService = configDepParamsService;
		this.deploymentService = deploymentService;
		this.configurationService = configurationService;
		this.deploymentConfigurationService = deploymentConfigurationService; 
		this.teamService = teamService;
		this.domainService = domainService;
	}

	@RequestMapping(method=RequestMethod.GET)
	public Resources<TeamResource> getAllTeamsForCurrentUser(Principal principal){

		Collection<Team> teams = teamService.findByAccountUsername(principal.getName());
		return new Resources<>(teams.stream().map(
				TeamResource::new
		).collect(Collectors.toList())
		);
	}

	@RequestMapping(value="/all",method=RequestMethod.GET)
	public Resources<TeamResource> getAllTeams(Principal principal){

		Collection<Team> teams = teamService.findAll();
		return teamService.populateTeamMemberEmails(teams, principal.getName());
	}

	@RequestMapping(method=RequestMethod.POST)
	public ResponseEntity<?> createNewTeam(HttpServletRequest request, HttpServletResponse response, Principal principal, @RequestBody TeamResource teamResource){
		logger.info("User " + principal.getName() + " requested creation of team " + teamResource.getName());

		if(teamResource.getName() == null || teamResource.getName().isEmpty()){
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		//name pattern is different , it does not contain '.' character.
		if(!teamResource.getName().matches("^[a-zA-Z0-9]+([\\s\\-\\_]?[a-zA-Z0-9]+)*")){
			logger.error("Input team name does not match required name pattern");
			throw new TeamNameInvalidInputException("Could not create configuration" + teamResource.getName() + "Invalid input."
					+ " Names should start only with an alphabet or number. "
					+ " Names may end with or contain '.' ,'_', '-' or spaces inbetween them.");
		}

		Team team = teamService.constructTeam(principal.getName(), teamResource, accountService, request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1] );

		if(team != null){
			logger.info("Team and domain created successfully");
			// Prepare response
			HttpHeaders httpHeaders = new HttpHeaders();
			return new ResponseEntity<>(new TeamResource(team), httpHeaders, HttpStatus.OK);
		}else{
			logger.info("Team and domain could not be created");
			// Prepare response
			throw new TeamNotCreatedException(teamResource.getName(), "failed to create domain");
		}
	}

	@RequestMapping(value="/{teamName:.+}", method=RequestMethod.GET)
	public TeamResource getTeamByName(HttpServletRequest request, Principal principal, @PathVariable String teamName){
		logger.info("User " + principal.getName() + " requested team " + teamName);

		if(teamName == null || teamName.isEmpty()){
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		Team team = null;
		try{
			team = teamService.findByName(teamName);
		}catch(TeamNotFoundException e) {
			Account account = accountService.findByUsername(principal.getName());
			Team memberTeam  = teamService.findByName(teamName);
			if(account.getMemberOfTeams().contains(memberTeam )){
				team = memberTeam;
			}
		}
		if(team == null){
			throw new TeamNotFoundException(teamName);
		}

		String token = getToken(request);
		TeamResource teamResource = teamService.setManagerUsernamesAndEmails(new TeamResource(team), token);
		teamResource = teamService.populateTeamContactEmails(team, teamResource, principal.getName());
		teamResource = teamService.populateTeamMemberEmails(team, teamResource, principal.getName());
		return teamResource;
	}

	@RequestMapping(value="/{teamName}", method=RequestMethod.DELETE)
	public ResponseEntity<?> deleteTeam(HttpServletRequest request, Principal principal, @PathVariable String teamName){
		logger.info("User " + principal.getName() + " requested deletion of team " + teamName);

		if(teamName == null || teamName.isEmpty()){
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

        logger.info("Checking if user is team owner");
        Team team = this.teamService.findByNameAndAccountUsername(teamName, principal.getName());
        teamService.deleteTeam(
                team,
                request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1],
                deploymentService,
                cloudProviderParametersCopyService,
                configurationService);

        return new ResponseEntity<>("Team " + teamName + " was deleted ",HttpStatus.OK);


	}

	@RequestMapping(value="/member", method=RequestMethod.POST)
	public ResponseEntity<?> addMember(HttpServletRequest request, Principal principal, @RequestBody TeamResource teamResource) throws AccountNotFoundException {
		logger.info("User " + principal.getName() + " requested adding members to team " + teamResource.getName());

		if(teamResource.getName() == null || teamResource.getName().isEmpty()){
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}
  

		logger.info("Checking if user is team owner");
		this.teamService.checkIfOwnerOrManagerOfTeam(teamResource.getName(), principal, getToken(request));
		String baseURL = this.composeBaseURL(request);
		teamService.addMemberToTeam(
				getToken(request),
				teamResource.getName(),
				teamResource.getMemberAccountEmails(),
				baseURL);
		return new ResponseEntity<>("User  was added to team and domain " + teamResource.getName(), HttpStatus.OK);

	}

	@RequestMapping(value="/member", method=RequestMethod.GET)
	public Resources<TeamResource> getMemberTeams(HttpServletRequest request, Principal principal){
		logger.info("User " + principal.getName() + " requested all teams she is a member");
		String token = getToken(request);

		List<Team> teams = teamService.findByAccountUsername(principal.getName()).stream().collect(Collectors.toList());
		Set<Team> memberTeams = new HashSet<>();
		memberTeams.addAll(teams);

		Account account = accountService.findByUsername(principal.getName());
		Collection<Team> allTeams  = teamService.findAll();

		for(Team team: allTeams){
			if(account.getMemberOfTeams().contains(team)){
				memberTeams.add(team);
			}
		}

		Collection<Domain> domainCollection = domainService.getMyManagementDomains(token);

		for(Domain domain: domainCollection){
			try {
				//Adding Management teams using Management domain
				memberTeams.add(teamService.findByDomainReference(domain.getDomainReference()));
			}catch(TeamNotFoundException e) {
				logger.info(e.getMessage());
			}
		}

		List<TeamResource> resourceList = new ArrayList<>();
		for (Team team: memberTeams){
			TeamResource teamResource = teamService.setManagerUsernamesAndEmails(new TeamResource(team), token);
			teamResource = teamService.populateTeamMemberEmails(team, teamResource, principal.getName());
			resourceList.add(teamResource);
		}
		return new Resources<>(resourceList);
	}

	@RequestMapping(value="/{teamName:.+}/member/{userEmail:.+}", method=RequestMethod.DELETE)
	public ResponseEntity<?> removeMemberFromTeam(HttpServletRequest request, Principal principal,
												  @PathVariable String teamName,
												  @PathVariable String userEmail) throws AccountNotFoundException {

        logger.info("Request to remove user " + userEmail + " from team " + teamName);

		if(teamName == null || teamName.isEmpty()){
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		Team team = this.teamService.checkIfOwnerOrManagerOfTeam(teamName, principal, getToken(request));
		teamService.removeMemberFromTeam(
				getToken(request), teamName, userEmail);

		teamService.stopDeploymentsByMemberUserEmail(team, userEmail);
		return new ResponseEntity<>("User " + userEmail + " was deleted from team " + teamName, HttpStatus.OK);

	}

	@RequestMapping(value="/{teamName:.+}/application", method=RequestMethod.POST)
	public ResponseEntity<?> addApplicationToTeam(Principal principal,
			@RequestBody ApplicationResource applicationResource, @PathVariable String teamName) {
		logger.info("User " + principal.getName() + " requested adding application " + applicationResource.getName()
		+ " to team " + teamName);

		if(teamName == null || teamName.isEmpty()){
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		try{
			logger.info("Checking if user is team owner");
			Team team = teamService.findByNameAndAccountUsername(teamName, principal.getName());
			logger.info("Checking if user owns the application");
			Application application =
					this.applicationService.findByAccountUsernameAndName(principal.getName(), applicationResource.getName());
			logger.info("User " + principal.getName() + " owns both entities...");
			team.getApplicationsBelongingToTeam().add(application);
			team = this.teamService.save(team);
			application.getSharedWithTeams().add(team);
			this.applicationService.save(application);
		}catch(TeamNotFoundException e){
			throw new RuntimeException("Team not found or you should be the team owner to add application to team");
		}catch(ApplicationNotFoundException e){
			throw new RuntimeException("Application not found or you should be the application owner to share application.");
		}

		return new ResponseEntity<>("Application " + "'" + applicationResource.getName() + "'" + "was shared with team " + "'" + teamName + "'"  , HttpStatus.OK);
	}

	@RequestMapping(value="/{teamName:.+}/application", method=RequestMethod.GET)
	public Resources<ApplicationResource> getAllTeamApplications(Principal principal, @PathVariable String  teamName){
		logger.info("User " + principal.getName() + " requested all applications for team " + teamName);

		if (teamName == null || teamName.isEmpty()) {
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		Team team = this.teamService.findByName(teamName);
		List<ApplicationResource> res = team.getApplicationsBelongingToTeam().stream().map(
				ApplicationResource::new
				).collect(Collectors.toList());

		return new Resources<>(res);

	}

	@RequestMapping(value="/{teamName:.+}/application/{applicationName:.+}", method=RequestMethod.DELETE)
	public ResponseEntity<?> removeApplicationFromTeam(Principal principal, @PathVariable String teamName,
			@PathVariable String applicationName){
		logger.info("User " + principal.getName() + " requested removing application "
				+ applicationName + " from team " + teamName);

		if (teamName == null || teamName.isEmpty()) {
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		if (applicationName == null || applicationName.isEmpty()) {
			throw new InvalidApplicationInputException(principal.getName(), applicationName);
		}

		try{
			logger.info("Checking if user is team owner");
			Team team = this.teamService.findByNameAndAccountUsername(teamName, principal.getName());
			logger.info("Checking if user owns the application");
			Application application = applicationService.findByAccountUsernameAndName(principal.getName(), applicationName);
			logger.info("User " + principal.getName() + " owns both entities...");
			if (team.getApplicationsBelongingToTeam().contains(application)) {
				// Update team
				team.getApplicationsBelongingToTeam().remove(application);
				// Update application
				application.getSharedWithTeams().remove(team);
				// Persist
				this.teamService.save(team);
				this.applicationService.save(application);
			}
		}catch(TeamNotFoundException e){
			throw new RuntimeException("Team not found or you should be team owner to remove application");
		}catch(ApplicationNotFoundException e){
			throw new RuntimeException("Application not found or you should be application owner to remove application");
		}
		return new ResponseEntity<>("Application " + "'" + applicationName + "'"
				+ "was deleted from team " + "'" + teamName + "'"  , HttpStatus.OK);
	}

	@RequestMapping(value="/{teamName:.+}/cloudproviderparameters", method=RequestMethod.POST)
	public ResponseEntity<?> addCloudProviderParametersToTeam(
			Principal principal,
			@RequestBody CloudProviderParametersResource cloudProviderParametersResource,
			@PathVariable String teamName)
					throws InvalidKeyException, NoSuchPaddingException,
					NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeySpecException,
					BadPaddingException, IllegalBlockSizeException{
		logger.info("User " + principal.getName() + " requested adding provider parameters "
				+ cloudProviderParametersResource.getName() + " to team " + teamName);

		if(teamName == null || teamName.isEmpty()){
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		try{
			logger.info("Checking if user is team owner");
			Team team = teamService.findByNameAndAccountUsername(teamName, principal.getName());
			logger.info("Checking if user owns the cloud provider parameters");
			CloudProviderParameters cloudProviderParameters =
					this.cloudProviderParametersService.findByNameAndAccountUsername(
							cloudProviderParametersResource.getName(), principal.getName());
			logger.info("User " + principal.getName() + " owns both entities...");
			// Update and persist
			team.getCppBelongingToTeam().add(cloudProviderParameters);
			this.teamService.save(team);
			cloudProviderParameters.getSharedWithTeams().add(team);
			this.cloudProviderParametersService.save(cloudProviderParameters);
		}catch(TeamNotFoundException e){
			throw new RuntimeException("Team not found or you should be the team owner to add cloud provider parameters to team");
		}catch(CloudProviderParametersNotFoundException e){
			throw new RuntimeException("CloudProviderParameters not found or user should own it to share it");
		}

		return new ResponseEntity<>("CloudProviderParameters " + "'"
				+ cloudProviderParametersResource.getName() + "'" + "was shared with team "
				+ "'" + teamName + "'"  , HttpStatus.OK);
	}

	@RequestMapping(value="/{teamName:.+}/cloudproviderparameters", method=RequestMethod.GET)
	public Resources<CloudProviderParametersResource> getAllTeamCloudProviderParameters(Principal principal, @PathVariable String  teamName){
		logger.info("User " + principal.getName() + " requested all provider parameters for team " + teamName);

		if(teamName == null || teamName.isEmpty()){
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		Team team = this.teamService.findByName(teamName);
		List<CloudProviderParametersResource> res = team.getCppBelongingToTeam().stream().map(
				CloudProviderParametersResource::new
				).collect(Collectors.toList());

		return new Resources<>(res);
	}

	@RequestMapping(value="/{teamName:.+}/cloudproviderparameters/{cloudProviderParameterName:.+}", method=RequestMethod.DELETE)
	public ResponseEntity<?> removeCloudProviderParametersFromTeam(
			Principal principal,
			@PathVariable String teamName, @PathVariable String cloudProviderParameterName)
					throws NoSuchPaddingException, InvalidAlgorithmParameterException,
					NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException,
					InvalidKeyException, InvalidKeySpecException, IOException {
		logger.info("User " + principal.getName() + " requested removing provider parameters "
				+ cloudProviderParameterName + " from team " + teamName);

		if(teamName == null || teamName.isEmpty()){
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		if(cloudProviderParameterName == null || cloudProviderParameterName.isEmpty()){
			throw new InvalidCloudProviderParametersInputException(principal.getName(), cloudProviderParameterName);
		}

		try{
			logger.info("Checking if user is team owner");
			Team team = teamService.findByNameAndAccountUsername(teamName, principal.getName());

			logger.info("Checking if user owns the cloud provider parameters");
			CloudProviderParameters cloudProviderParameters =
					this.cloudProviderParametersService.findByNameAndAccountUsername(
							cloudProviderParameterName, principal.getName());
			logger.info("User " + principal.getName() + " owns both entities...");

			//stop deployments using shared credential
			CloudProviderParameters toUnshare = cloudProviderParameters;
			this.teamService.stopDeploymentsUsingGivenTeamSharedCloudProvider(team, toUnshare);

			// Update team
			team.setCppBelongingToTeam(
					team.getCppBelongingToTeam().stream().filter(
							ccp -> !(ccp.getAccount().getUsername().equals(principal.getName()) &&
									ccp.getName().equals(cloudProviderParameterName))
							).collect(Collectors.toSet())
					);

			// Update provider parameters
			cloudProviderParameters.getSharedWithTeams().remove(team);

			// Persist
			team = this.teamService.save(team);
			cloudProviderParameters = this.cloudProviderParametersService.save(cloudProviderParameters);


		}catch(TeamNotFoundException e){
			throw new RuntimeException("Team not found or you should be the team owner to remove cloud provider parameters to team");
		}catch(CloudProviderParametersNotFoundException e){
			throw new RuntimeException("CloudProviderParameters not found or user should own it to delete it from team");
		}

		return new ResponseEntity<>("Cloud Provider Parameter " + "'" + cloudProviderParameterName + "'" + "was deleted from team " + "'" + teamName + "'"  , HttpStatus.OK);
	}

    @Deprecated
	@RequestMapping(value="/{teamName:.+}/configurationdeploymentparameters", method=RequestMethod.POST)
	public ResponseEntity<?> addConfigurationDeploymentParametersToTeam(Principal principal,
			@RequestBody ConfigurationDeploymentParametersResource configDepParamsResource, @PathVariable String teamName) throws MethodNotSupportedException {

		logger.info("User " + principal.getName() + " requested adding configuration deployment parameters to team " + configDepParamsResource.getName()
		+ " to team " + teamName);

		throw new MethodNotSupportedException("Sharing configuration parameters is deprecated - share Configurations instead");
	}

	@RequestMapping(value="/{teamName:.+}/configurationdeploymentparameters", method=RequestMethod.GET)
	public Resources<ConfigurationDeploymentParametersResource> getAllTeamConfigurationDeploymentParameters(Principal principal, @PathVariable String  teamName){
		logger.info("User " + principal.getName() + " requested all configuration deployment parameters for team " + teamName);

		if (teamName == null || teamName.isEmpty()) {
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		Team team = this.teamService.findByName(teamName);
		List<ConfigurationDeploymentParametersResource> res = team.getConfigDepParamsBelongingToTeam().stream().map(
				ConfigurationDeploymentParametersResource::new
				).collect(Collectors.toList());

		return new Resources<>(res);

	}

	@Deprecated
	@RequestMapping(value="/{teamName:.+}/configurationdeploymentparameters/{configDepParamsName:.+}", method=RequestMethod.DELETE)
	public ResponseEntity<?> removeConfigurationDeploymentParametersFromTeam(Principal principal, @PathVariable String teamName,
			@PathVariable String configDepParamsName) throws IOException{
		logger.info("User " + principal.getName() + " requested removing configuration deployment parameters "
				+ configDepParamsName + " from team " + teamName);

		if (teamName == null || teamName.isEmpty()) {
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		if (configDepParamsName == null || configDepParamsName.isEmpty()) {
			throw new InvalidConfigurationDeploymentParametersException(principal.getName(), configDepParamsName);
		}

		try{
			logger.info("Checking if user is team owner");
			Team team = teamService.findByNameAndAccountUsername(teamName, principal.getName());
			logger.info("Checking if user owns configuration deployment parameters");
			ConfigurationDeploymentParameters configurationDeploymentParameters =
					this.configDepParamsService.findByNameAndAccountUserName(configDepParamsName, principal.getName());
			logger.info("User " + principal.getName() + " owns both entities...");

			logger.info("Removing the shared deployment parameters from team");

			// Update team
			team.getConfigDepParamsBelongingToTeam().remove(configurationDeploymentParameters);

			// Update configurationDeploymentParameters
			configurationDeploymentParameters.getSharedWithTeams().remove(team);

			// Persist
			this.teamService.save(team);
			this.configDepParamsService.save(configurationDeploymentParameters);
		}catch(TeamNotFoundException e){
			throw new RuntimeException("You should be team owner to remove deployment parameters from team");
		}catch(ConfigurationDeploymentParametersNotFoundException e){
			throw new RuntimeException("You should be the owner of the deployment parameters to remove it from team");
		}
		return new ResponseEntity<>("ConfigurationDeploymentParameters " + "'" + configDepParamsName + "'"
				+ "was deleted from team " + "'" + teamName + "'"  , HttpStatus.OK);
	}

	@RequestMapping(value="/{teamName:.+}/configuration", method=RequestMethod.POST)
	public ResponseEntity<?> addConfigurationToTeam(Principal principal,
			@RequestBody ConfigurationResource configResource, @PathVariable String teamName) {

		logger.info("User " + principal.getName() + " requested adding configuration to team " + configResource.getName()
		+ " to team " + teamName);

		if(teamName == null || teamName.isEmpty()){
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		try{
			logger.info("Checking if user is team owner");
			Team team = teamService.findByNameAndAccountUsername(teamName, principal.getName());
			logger.info("Checking if user owns configuration");
			Configuration configuration = this.configurationService.findByNameAndAccountUsername(configResource.getName(), principal.getName());
			logger.info("User " + principal.getName() + " owns both entities...");
			try{
				this.cloudProviderParametersService.findByNameAndAccountUsername(configuration.getCloudProviderParametersName(), principal.getName());
				this.configDepParamsService.findByReference(configuration.getConfigDeployParamsReference());
				team.getConfigurationsBelongingToTeam().add(configuration);
				team = this.teamService.save(team);
				configuration.getSharedWithTeams().add(team);
				this.configurationService.save(configuration);
			}catch(Exception e){
				if(e.getClass().equals(CloudProviderParametersNotFoundException.class)
						|| e.getClass().equals(ConfigurationDeploymentParametersNotFoundException.class)){
					throw new RuntimeException("You should own both cloud credential and deployment parameters, in order to share configuration");
				}
			}
		}catch(TeamNotFoundException e){
			throw new RuntimeException("You should be team owner to add configuration to team");
		}catch(ConfigurationNotFoundException e){
			throw new RuntimeException("Configuration not found or you should own it to share it");
		}

		return new ResponseEntity<>("Configuration " + "'" + configResource.getName() + "'" + "was shared with team " + "'" + teamName + "'"  , HttpStatus.OK);
	}

	@RequestMapping(value="/{teamName:.+}/configuration", method=RequestMethod.GET)
	public Resources<ConfigurationResource> getAllTeamConfigurations(Principal principal, @PathVariable String  teamName){
		logger.info("User " + principal.getName() + " requested all configurations for team " + teamName);

		if (teamName == null || teamName.isEmpty()) {
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		Team team = this.teamService.findByName(teamName);
		List<ConfigurationResource> res = this.configurationService.createConfigurationResource(team.getConfigurationsBelongingToTeam());
		return new Resources<>(res);

	}

	@RequestMapping(value="/{teamName:.+}/configuration/{configName:.+}", method=RequestMethod.DELETE)
	public ResponseEntity<?> removeConfigurationFromTeam(Principal principal, @PathVariable String teamName,
			@PathVariable String configName) throws IOException{
		logger.info("User " + principal.getName() + " requested removing configuration "
				+ configName + " from team " + teamName);

		if (teamName == null || teamName.isEmpty()) {
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		if (configName == null || configName.isEmpty()) {
			throw new InvalidConfigurationInputException(principal.getName(), configName);
		}

		try{
			logger.info("Checking if user is team owner");
			Team team = teamService.findByNameAndAccountUsername(teamName, principal.getName());
			logger.info("Checking if user owns configuration");
			Configuration configuration =
					this.configurationService.findByNameAndAccountUsername(configName, principal.getName());

			logger.info("User " + principal.getName() + " owns both entities...");

			//stop deployments using shared configuration
			this.teamService.stopDeploymentsUsingGivenTeamSharedConfiguration(team, configuration);

			logger.info("Removing configuration from team");

			// Update team
			team.getConfigurationsBelongingToTeam().remove(configuration);

			// Update configuration
			configuration.getSharedWithTeams().remove(team);

			// Persist
			this.teamService.save(team);
			this.configurationService.save(configuration);

		}catch(TeamNotFoundException e){
			throw new RuntimeException("Team not found or you should be its owner to delete configuration from team");
		}catch(ConfigurationNotFoundException e){
			throw new RuntimeException("Configuration not found or you should its owner to delete it from team");
		}
		return new ResponseEntity<>("Configuration " + "'" + configName + "'"
				+ "was deleted from team " + "'" + teamName + "'"  , HttpStatus.OK);
	}

	@RequestMapping(value="/join", method=RequestMethod.POST)
	public ResponseEntity<?> addMemberOnRequest(HttpServletRequest request, Principal principal, @RequestBody TeamResource teamResource) throws IOException{

		logger.info("User " + principal.getName() + " requested to join team " + teamResource.getName());

		if(teamResource.getName() == null || teamResource.getName().isEmpty()){
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		try{
			this.teamService.addMemberOnRequest(this.composeBaseURL(request), teamResource);
		}catch(IndexOutOfBoundsException e){
			throw new RuntimeException("User is already a member of the team");
		}

		return new ResponseEntity<>("User  request was successfully sent to team owner " + teamResource.getName(), HttpStatus.OK);
	}

	@RequestMapping(value = "/{teamName:.+}/contactemail/", method = RequestMethod.PUT)
	public ResponseEntity<?> addTeamContactEmails(HttpServletRequest request, Principal principal,
												  @PathVariable("teamName") String teamName,
												  @RequestBody String emails) {
		logger.info("User " + principal.getName() + " requested adding contacts to team " + teamName);

		if (teamName == null || teamName.isEmpty()) {
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		logger.info("Checking if user is team owner");
		Team team = teamService.findByName(teamName);
		if (!team.getAccount().getUsername().equals(principal.getName())) {
			throw new TeamAccessDeniedException(team.getName());
		}
		Set<String> emailSet = Arrays.stream(emails.split(",")).collect(Collectors.toSet());
		team = teamService.setContactEmails(emailSet, team);
		return new ResponseEntity<>(new TeamResource(team), HttpStatus.OK);

	}

	@RequestMapping(value = "/{teamName:.+}/contactemail/{userEmail:.+}", method = RequestMethod.DELETE)
	public ResponseEntity<?> removeTeamContactEmail(HttpServletRequest request, Principal principal,
													@PathVariable String teamName,
													@PathVariable String userEmail) {

		logger.info("Request to remove contact " + userEmail + " from team " + teamName);

		if (teamName == null || teamName.isEmpty()) {
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		logger.info("Checking if user is team owner");
		Team team = teamService.findByName(teamName);
		if (!team.getAccount().getUsername().equals(principal.getName())) {
			throw new TeamAccessDeniedException(team.getName());
		}

		teamService.removeTeamContactEmail(team, userEmail);

		return new ResponseEntity<>(HttpStatus.NO_CONTENT);

	}

	public String composeBaseURL(HttpServletRequest request) {
		
		String baseURL = this.getBaseURL(request);
		if(baseURL.contains(".api")){
			//for dev
			baseURL = baseURL.replace(".api", "");	
			logger.info("Base URL, removed api is " + baseURL);
		}else{
			if(baseURL.contains("api")){
				//for master
				baseURL = baseURL.replace("api.", "");	
				logger.info("Base URL, removed api is " + baseURL);
			}
		}
		return baseURL;
	}
	
	public String getBaseURL(HttpServletRequest request) {

		StringBuffer url = request.getRequestURL();
		String uri = request.getRequestURI();
		String ctx = request.getContextPath();
		String base = url.substring(0, url.length() - uri.length() + ctx.length()) + "/";
		return base;

	}
	
	private static String getToken(HttpServletRequest request) {
		return request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
	}

	
	//functionality to be included in aap side
	/*@RequestMapping(value="/leave", method=RequestMethod.POST)
	public ResponseEntity<?> leaveTeam(HttpServletRequest request, Principal principal, @RequestBody TeamResource teamResource){

		logger.info("User " + principal.getName() + " requested to leave team " + teamResource.getName());

		if(teamResource.getName() == null || teamResource.getName().isEmpty()){
			throw new TeamNameInvalidInputException("Team name should not be empty");
		}

		try{

			boolean removedUser = teamService.leaveTeam(
					request, 
					deploymentService,
					configurationService, 
					configDepParamsService, 
					teamResource
					);

			if(removedUser ){
				return new ResponseEntity<>("User was successfully removed from team '" + teamResource.getName() + "'.", HttpStatus.OK);
			}else{
				return new ResponseEntity<>("Failed to remove user from team " + teamResource.getName(), HttpStatus.NOT_MODIFIED);
			}

		}catch(TeamNotFoundException e){
			logger.error("In leaveTeam: Could not find team '"+teamResource.getName()+"'.");
			return new ResponseEntity<>("Team '" + teamResource.getName() + "' not found.", HttpStatus.NOT_MODIFIED);
		}catch(Exception e){
			e.printStackTrace();
			return new ResponseEntity<>("Failed to remove user from team " + teamResource.getName(), HttpStatus.NOT_MODIFIED);
		}

	}*/
}
