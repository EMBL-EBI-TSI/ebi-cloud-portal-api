package uk.ac.ebi.tsc.portal.api.team.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.repo.AccountRepository;
import uk.ac.ebi.tsc.portal.api.account.service.AccountService;
import uk.ac.ebi.tsc.portal.api.account.service.UserNotFoundException;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopy;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyService;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationDeploymentParametersService;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationService;
import uk.ac.ebi.tsc.portal.api.deployment.repo.*;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentConfigurationService;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.team.controller.TeamResource;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;
import uk.ac.ebi.tsc.portal.api.team.repo.TeamRepository;
import uk.ac.ebi.tsc.portal.api.utils.SendMail;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDeployerBash;
import uk.ac.ebi.tsc.portal.clouddeployment.exceptions.ApplicationDeployerException;

import javax.security.auth.login.AccountNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Navis Raj <navis@ebi.ac.uk>
 * @author Jose A. Dianes <jdianes@ebi.ac.uk> (code refactoring)
 */
public class TeamService {

	private final TeamRepository teamRepository;
	private final AccountService accountService;
	private final DomainService domainService;
	private final DeploymentService deploymentService;
	private final CloudProviderParamsCopyService cloudProviderParametersCopyService;
	private final DeploymentConfigurationService deploymentConfigurationService;
	private final ApplicationDeployerBash applicationDeployerBash;

	private static final Logger logger = LoggerFactory.getLogger(TeamService.class);

	@Autowired
	public TeamService(TeamRepository teamRepository,
			AccountRepository accountRepository, 
			DomainService domainService,
			DeploymentService deploymentService,
			CloudProviderParamsCopyService cloudProviderParametersCopyService,
			DeploymentConfigurationService deploymentConfigurationService,
			ApplicationDeployerBash applicationDeployerBash
			){
		this.teamRepository = teamRepository;
		this.accountService = new AccountService(accountRepository);
		this.domainService = domainService;
		this.deploymentService = deploymentService ;
		this.cloudProviderParametersCopyService = cloudProviderParametersCopyService;
		this.deploymentConfigurationService = deploymentConfigurationService;
		this.applicationDeployerBash = applicationDeployerBash;

	}

	public Team findByName(String name){
		return this.teamRepository.findByName(name).orElseThrow(() -> new TeamNotFoundException(name));
	}

	public Collection<Team> findByAccountUsername(String accountUsername){
		return this.teamRepository.findByAccountUsername(accountUsername);
	}

	public Team save(Team team) {
		return this.teamRepository.save(team);
	}

	public void delete(Team team){
		this.teamRepository.delete(team);
	}


	public Team findByNameAndAccountUsername(String teamName, String name) {
		return this.teamRepository.findByNameAndAccountUsername(teamName,name).orElseThrow(() -> new TeamNotFoundException(name));
	}

	public Collection<Team> findAll(){
		return this.teamRepository.findAll();
	}

	public Team findByDomainReference(String domainReference) {
		return this.teamRepository.findByDomainReference(domainReference).orElseThrow(() -> new TeamNotFoundException("with domain reference " + domainReference));
	}

	public Team constructTeam(
			String userName,
			TeamResource teamResource, 
			AccountService accountService,
			String token) throws UserNotFoundException {


		logger.info("Creating team " + teamResource.getName() + " for user " + userName);

		// Create team
        logger.info("Creating new team");
        Team team = new Team();
        team.setName(teamResource.getName());

        team.setAccount(accountService.findByUsername(userName));
        Account ownerAccount = team.getAccount();

        // Add team members if needed
        if (teamResource.getMemberAccountEmails()!=null) {
            Set<Account> memberAccounts = teamResource.getMemberAccountEmails().stream()
                    .map(email -> accountService.findByEmail(email)).collect(Collectors.toSet());
            memberAccounts.add(ownerAccount);
            team.setAccountsBelongingToTeam(memberAccounts);
        }

        // Create associated domain
        // Form domain name
        String domainName = "TEAM_"+ teamResource.getName().toUpperCase()+"_PORTAL_" + userName.toUpperCase();
        try {
            Domain domain = domainService.createDomain(domainName, "Domain " + domainName + " created", token);

            logger.info("Created domain " + domain.getDomainName());

            team.setDomainReference(domain.getDomainReference());

            logger.info("In TeamService: Created team, now saving it " + team.getName());

            try {
                this.save(team);
                return team;
            } catch (Exception e) {
                logger.error("Failed to persist team, after creating AAP domain, so deleting domain: " + e.getMessage());
                try {
                    domainService.deleteDomain(domain, token);
                } catch(Exception ex){
                    logger.error("Failed to delete the already created AAP domain: " + ex.getMessage());
                    throw new TeamNotCreatedException(teamResource.getName(), "failed to persist team and delete already created domain");
                }

                throw new TeamNotCreatedException(teamResource.getName(), "failed to persist team");
            }

        } catch (Exception e) {
            logger.error("Failed to create AAP domain " + domainName);
            throw new TeamNotCreatedException(teamResource.getName(), "failed to create domain " + domainName + ". Reason: " + e.getMessage());
        }



	}

	public Team removeMemberFromTeam(
			String token,
			String teamName, 
			String userEmail) throws AccountNotFoundException {

		logger.debug("Removing user from team " + teamName);

		// Get team
		Team team = this.findByName(teamName);
		if (team==null) {
			throw new TeamNotFoundException(teamName);
		}

		// Get account
		Account account = accountService.findByEmail(userEmail);
		if (account==null) {
			throw new AccountNotFoundException();
		}

		// Only proceed if account is member of team
        if (!team.getAccountsBelongingToTeam().contains(account)) {
		    throw new TeamMemberNotRemovedException(teamName, "account " +
                    account.getReference() + " is not a member of the team");
        }

		logger.debug("Found associated user with reference " + account.getUsername());

		// If there is an associated domain (it should not for legacy teams)
		if (team.getDomainReference() != null) {
			try {
				//remove member from domain
				logger.debug("Removing user from domain " + team.getDomainReference());

				Domain domain = domainService.getDomainByReference(team.getDomainReference(), token);
				if (domain != null) {
					User domainUser = domainService.getAllUsersFromDomain(domain.getDomainReference(), token)
							.stream()
							.filter(u -> u.getEmail().equals(userEmail))
							.findAny()
							.orElse(null);
					// If the account is actually part of the domain, update the domain
					if (domainUser != null) {
						this.domainService.removeUserFromDomain(new User(null,
										account.getEmail(),
										account.getUsername(),
										account.getGivenName(),
										null),
								domain,
								token);
					}
				}
			} catch (HttpClientErrorException e) {
				if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
					logger.debug("Domain does not exist, removing user from team ");
				} else {
					e.printStackTrace();
					logger.error("Error removing user from team and domain "  + e.getMessage());
				}
			}
		}

		// Update account
		account.getMemberOfTeams().remove(team);
		this.accountService.save(account);

		// Update team
		team.getAccountsBelongingToTeam().remove(account);
		return this.save(team);
	}

	public Team addMemberToTeam(
			String token,
			String teamName,
			Collection<String> newMemberEmails,
			String baseURL) throws AccountNotFoundException, UserNotFoundException {

		logger.info("Adding member to team " + teamName);

        // Get team
        Team team = this.findByName(teamName);
        if (team==null) {
            throw new TeamNotFoundException(teamName);
        }

        // get the future member accounts
        logger.info("Checking if user is already a member");
        Set<String> memberAccountEmails = team.getAccountsBelongingToTeam().stream().map(Account::getEmail).collect(Collectors.toSet());
        Collection<String> yetToBeMemberEmails = newMemberEmails.stream().filter(
                a -> !memberAccountEmails.contains(a)).collect(Collectors.toSet());
        // throw exception if not all accounts can be added
        if (newMemberEmails.size()!=yetToBeMemberEmails.size()) {
            throw new TeamMemberNotAddedException(teamName, "some accounts already belong to the team, nothing added");
        }
        // get the accounts
        Collection<Account> yetToBeMemberAccounts = yetToBeMemberEmails.stream().map(
                email -> accountService.findByEmail(email)).collect(Collectors.toSet());
        // throw exception if not all accounts can be found
        if (newMemberEmails.size()!=yetToBeMemberAccounts.size()) {
            throw new TeamMemberNotAddedException(teamName, "some accounts cannot be found, nothing added");
        }

        // add accounts to team and persist
        yetToBeMemberAccounts.stream().forEach(
                account -> {
                    // Update domain if needed
                    if (team.getDomainReference() != null) {
                        logger.info("Team has associated domain " + team.getDomainReference() + ". Updating...");
                        logger.info("Getting domain");
                        Domain domain = domainService.getDomainByReference(
                                team.getDomainReference(),
                                token);
                        if (domain != null) {
                            domainService.addUserToDomain(
                                    domain,
                                    new User(null, account.getEmail(), account.getUsername(), account.getGivenName() , null),
                                    token
                            );

                        } else {
                            throw new TeamMemberNotAddedException(teamName, "cannot find associated domain " + team.getDomainReference());
                        }
                    }

                    //update account
                    account.getMemberOfTeams().add(team);
                    account = this.accountService.save(account);

                    // update team
                    team.getAccountsBelongingToTeam().add(account);
                    this.save(team);

                    // Send email notification
                    String loginURL = baseURL + "login";
                    String teamURL = baseURL + "team" + "/" + team.getName() ;
                    String message = "User " + team.getAccount().getGivenName() +
                            " has added you to the team " + "'"+team.getName()+"'" + ".\n\n"
                            + "Please click on the link below to login if you haven't already \n"
                            + loginURL.replaceAll(" ", "%20") + "\n\nor click on " + teamURL.replaceAll(" ", "%20")  + "\n"
                            + "to view the team.";
                    String toNotifyEmail = account.getEmail();

                    try {
                        SendMail.send(new ArrayList<String>(){{
                            add(toNotifyEmail);
                        }}, "Request granted: Added to team " + team.getName(), message );
                    } catch (IOException e) {
                        logger.error("Couldn't send notification email");
                        e.printStackTrace();
                    }
                }

        );

		return team;
	}

	public Team addMemberToTeamByAccountNoNotification(
	        String token,
            String teamName,
            Account account) {

		logger.info("Adding " + account.getReference() + " (" + account.getEmail() + ") to team " + teamName);

        // Get team
        Team team = this.findByName(teamName);
        if (team==null) {
            throw new TeamNotFoundException(teamName);
        }

        // Update domain if needed
        if (team.getDomainReference() != null) {
            logger.info("Team has associated domain " + team.getDomainReference() + ". Updating...");
            logger.info("Getting domain");
            Domain domain = domainService.getDomainByReference(
                    team.getDomainReference(),
                    token);
            if (domain != null) {
                domainService.addUserToDomain(
                        domain,
                        new User(null, account.getEmail(), account.getUsername(), account.getGivenName() , null),
                        token
                );

            } else {
                throw new TeamMemberNotAddedException(teamName, "cannot find associated domain " + team.getDomainReference());
            }
        }

        //update account
        account.getMemberOfTeams().add(team);
        account = this.accountService.save(account);

        // update team
        team.getAccountsBelongingToTeam().add(account);
        this.save(team);

        return team;
	}

	public boolean deleteTeam(
			Team team, 
			String token, 
			DeploymentService deploymentService,
			CloudProviderParamsCopyService cloudProviderParametersCopyService, 
			ConfigurationService configurationService) {

        logger.info("Deleting team " + team.getName());

		this.stopDeploymentsUsingTeamSharedCloudProvider(team, deploymentService, cloudProviderParametersCopyService);
		this.stopDeploymentsUsingTeamSharedConfigurationDeploymentParameters(team, deploymentService, configurationService);
		this.stopDeploymentsUsingTeamSharedConfigurations(team, deploymentService);

		if (team.getDomainReference() != null) {
            logger.info("- with domain reference " + team.getDomainReference());
			try {
				logger.info("Deleting team which has domain reference, getting domain");
				Domain domain = domainService.getDomainByReference(team.getDomainReference(), token);
				if (domain != null) {
                    logger.info("Deleting domain");
                    domain.setDomainReference(domain.getDomainReference());
                    domainService.deleteDomain(domain, token);
                }
                this.delete(team);
                return true;

			}catch(HttpClientErrorException e){
				if(e.getStatusCode().equals(HttpStatus.NOT_FOUND)){
					logger.error("In TeamService: domain was not found, deleting team" + e.getMessage());
					this.delete(team);
					return true;
				}else{
					logger.error("In TeamService: error deleting team and domain " + e.getMessage());
					e.printStackTrace();
					return false;
				}
			}catch(Exception e){
				logger.error("In TeamService: error deleting team and domain " + e.getMessage());
				return false;
			}
		}else{
			logger.error("In TeamService: team has no domain refrence, deleting team" );
			try{
				this.delete(team);
				return true;
			}catch(Exception e){
				logger.error("In TeamService: error deleting team that has no domain refrence" );
				return false;
			}
		}
	}

	public void stopDeploymentsUsingTeamSharedCloudProvider(
			Team team, 
			DeploymentService deploymentService,
			CloudProviderParamsCopyService cppCopyService ){

		logger.info("Stopping deployments of team members(not team owner's), using team's shared cloud parameters, "
				+ "before deleting team.");

		Set<Account> memberAccounts = team.getAccountsBelongingToTeam();
		Set<CloudProviderParameters> teamSharedCPP = team.getCppBelongingToTeam();
		List<String> toNotify = new ArrayList<>();
		memberAccounts.forEach(memberAccount -> {
			deploymentService.findAll().forEach(deployment -> {
				if(deployment.getAccount().getId().equals(memberAccount.getId()) &&
						(deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.RUNNING)
								|| deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.STARTING))){
					teamSharedCPP.forEach(cpp -> {
						CloudProviderParamsCopy cppCopy = cppCopyService.findByCloudProviderParametersReference(cpp.getReference());
						if(cppCopy.getCloudProviderParametersReference().equals(deployment.getCloudProviderParametersReference())
								&& !deployment.getAccount().getId().equals(team.getAccount().getId())){
							logger.info("Found deployments in running/starting status, using the cloud provider '" +
									cpp.getName() + "' shared with team '" + team.getName() + "'." );
							try{
								this.stopDeploymentByReference(deployment.getAccount().getUsername(), deployment.getReference());
								toNotify.add(deployment.getAccount().getEmail());
							}catch(Exception e){
								logger.error("Failed to stop deployment, using team's shared cloud credentials.");
							}
						}
					});
				}
			});
		});

		if(!toNotify.isEmpty()){
			logger.info("There are users who are to be notified, regarding deployments destruction");
			String message = "Your deployments were destroyed. \n"
					+ "This was because the team was deleted" + "'" + team.getName() + "'.\nYou had used team"
					+ " shared cloud credential for deployment" ;
			try {
				SendMail.send(toNotify, "Deployments destroyed", message );
			} catch (IOException e) {
				logger.error("Failed to send, deployments destroyed notification to the members of the team"
						+ " which was deleted" );
			}
		}
	}

	public void stopDeploymentsUsingTeamSharedConfigurationDeploymentParameters(
			Team team, 
			DeploymentService deploymentService,
			ConfigurationService configurationService){

		logger.info("Stopping deployments of team members(not team owner's), using team's shared configuration"
				+ "deployment parameters, before deleting team.");

		Set<Account> memberAccounts = team.getAccountsBelongingToTeam();
		Set<ConfigurationDeploymentParameters> teamSharedCDP = team.getConfigDepParamsBelongingToTeam();
		List<String> toNotify = new ArrayList<>();
		memberAccounts.forEach(memberAccount -> {
			deploymentService.findAll().forEach(deployment -> {
				if(deployment.getAccount().getId().equals(memberAccount.getId()) &&
						(deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.RUNNING)
								|| deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.STARTING))){
					teamSharedCDP.forEach(cdp -> {
						List<Configuration> configurations = configurationService.findAll().stream().filter(config -> config.getConfigDeployParamsReference()
								.equals(cdp.getReference())).collect(Collectors.toList());
						configurations.forEach(configuration -> {
							if(configuration.getReference().equals(deployment.getDeploymentConfiguration().getConfigurationReference())
									&& !deployment.getAccount().getId().equals(team.getAccount().getId())){
								logger.info("Found deployments in running/starting status, using the configuration deployment parameter '" +
										cdp.getName() + "' shared with team '" + team.getName() + "'." );
								try{
									this.stopDeploymentByReference(deployment.getAccount().getUsername(), deployment.getReference());
									toNotify.add(deployment.getAccount().getEmail());
								}catch(Exception e){
									logger.error("Failed to stop deployment, using team's shared configuration deployment parameters.");
								}
							}
						});
					});
				}
			});
		});

		if(!toNotify.isEmpty()){
			logger.info("There are users who are to be notified, regarding deployments destruction");
			String message = "Your deployments were destroyed. \n"
					+ "This was because you the team was deleted" + "'" + team.getName() + "'.\nYou had used team"
					+ " shared deployment parameter for deployment." ;
			try {
				SendMail.send(toNotify, "Deployments destroyed", message );
			} catch (IOException e) {
				logger.error("Failed to send, deployments destroyed notification to the members of the team"
						+ " which was deleted." );

			}
		}
	}

	public void stopDeploymentsUsingTeamSharedConfigurations(
			Team team, 
			DeploymentService deploymentService){

		logger.info("Stopping deployments of team members(not team owner's), using team's shared configurations"
				+ ", before deleting team.");

		Set<Account> memberAccounts = team.getAccountsBelongingToTeam();
		Set<Configuration> teamSharedConfiguration = team.getConfigurationsBelongingToTeam();

		List<String> toNotify = new ArrayList<>();
		memberAccounts.forEach(memberAccount -> {
			deploymentService.findAll().forEach(deployment -> {
				if(deployment.getAccount().getId().equals(memberAccount.getId()) &&
						(deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.RUNNING)
								|| deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.STARTING))){
					teamSharedConfiguration.forEach(configuration -> {
						if(configuration.getReference().equals(deployment.getDeploymentConfiguration().getConfigurationReference())
								&& !deployment.getAccount().getId().equals(team.getAccount().getId())){
							logger.info("Found deployments in running/starting status, using the configuration '" +
									configuration.getName() + "' shared with team '" + team.getName() + "'." );
							try{
								this.stopDeploymentByReference(deployment.getAccount().getUsername(), deployment.getReference());
								toNotify.add(deployment.getAccount().getEmail());
							}catch(Exception e){
								logger.error("Failed to stop deployment, using team's shared configurations.");
							}
						}
					});
				}
			});
		});

		if(!toNotify.isEmpty()){
			logger.info("There are users who are to be notified, regarding deployments destruction");
			String message = "Your deployments were destroyed. \n"
					+ "This was because you the team was deleted" + "'" + team.getName() + "'.\nYou had used team"
					+ " shared configuration for deployment." ;
			try {
				SendMail.send(toNotify, "Deployments destroyed", message );
			} catch (IOException e) {
				logger.error("Failed to send, deployments destroyed notification to the members of the team"
						+ " which was deleted." );

			}
		}
	}

	public void stopDeploymentsOfRemovedUser(Team team, 
			String userEmail, 
			DeploymentService deploymentService,
			ConfigurationService configurationService,
			ConfigurationDeploymentParametersService deploymentParametersService
			) {

		Account removedAccount  = accountService.findByEmail(userEmail);
		Collection<Deployment> deployments = deploymentService.findByAccountUsername(removedAccount.getUsername()).stream()
				.filter(deployment -> deployment.getDeploymentStatus().getStatus().equals(DeploymentStatusEnum.RUNNING) ||
						deployment.getDeploymentStatus().getStatus().equals(DeploymentStatusEnum.STARTING)).collect(Collectors.toList());

        logger.debug("Stopping all deployments associated with team " + team.getName() + " for user " + removedAccount.getReference());

		List<String> cloudParameters = team.getCppBelongingToTeam().stream().map(cpp -> cpp.getReference()).collect(Collectors.toList());
		Set<ConfigurationDeploymentParameters> deploymentParameters = team.getConfigDepParamsBelongingToTeam();
		Set<Configuration> configurations = team.getConfigurationsBelongingToTeam();

		deployments.forEach(deployment -> {

			boolean removeDeployment = false;
			//check if cloud credential from team is being used
			if(cloudParameters.contains(deployment.getCloudProviderParametersReference())){
				removeDeployment = true;
			}

			DeploymentConfiguration deploymentConfiguration = deployment.getDeploymentConfiguration();
			Configuration configuration = configurationService.findByReference(deploymentConfiguration.getConfigurationReference());

			//check if configuration from team is being used
			if(configurations.contains(configuration)){
				removeDeployment = true;
			}

			//check if configuration deployment parameters from team is being used
			ConfigurationDeploymentParameters cdp = deploymentParametersService.findByReference(configuration.getConfigDeployParamsReference());
			if(deploymentParameters.contains(cdp)){
				removeDeployment = true;
			}

			List<String> toNotify = new ArrayList<>();
			if(removeDeployment){
				try{
					if(deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.RUNNING)
							|| deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.STARTING)){
						this.stopDeploymentByReference(deployment.getAccount().getUsername(), deployment.getReference());
						toNotify.add(deployment.getAccount().getEmail());
					}
				}catch(Exception e){
					logger.error("Failed to stop deployment, on removing member from team");
				}
			}

			if(!toNotify.isEmpty()){
				logger.info("There are users who are to be notified, regarding deployments destruction");
				String message = "Your deployments were destroyed. \n"
						+ "This was because you were removed from the team " + "'" + team.getName() + "'." ;
				try {
					SendMail.send(toNotify, "Deployments destroyed", message );
				} catch (IOException e) {
					logger.error("Failed to send, deployments destroyed notification to " + removedAccount.getGivenName() + ".");
				}
			}

		});


	}

	public void stopDeploymentsUsingGivenTeamSharedCloudProvider(Team team,
			DeploymentService deploymentService,
			CloudProviderParameters toUnshare) {

		logger.info("Stopping deployments of team members(not owner's) on unsharing a cloud credential.");
		logger.info("Get member accounts of team " + team.getName());
		Set<Account> memberAccounts = team.getAccountsBelongingToTeam();
		logger.info("Find all deployments using the shared cloud credentials");
		List<Deployment> deployments = new ArrayList<>();
		deploymentService.findAll().forEach(d -> {
			logger.info("Deployment cloud reference " + d.getCloudProviderParametersReference());
			logger.info("To unshare cpp " + toUnshare.getReference());
			if(d.getCloudProviderParametersReference() != null){
				if(d.getCloudProviderParametersReference().equals(toUnshare.getReference())){
					logger.info("Adding deployment");
					deployments.add(d);
				}
			}
		});
		List<String> toNotify = new ArrayList<>();
		if(team.getCppBelongingToTeam().stream().map(cpp -> cpp.getId()).collect(Collectors.toList()).contains(toUnshare.getId())){
			deployments.forEach(deployment -> {
				if(deployment.getAccount().getId() != toUnshare.getAccount().getId() && 
						memberAccounts.stream().map(account -> account.getId()).collect(Collectors.toList()).contains(deployment.getAccount().getId())){
					try{
						if(deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.RUNNING)
								|| deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.STARTING)){
							logger.info("Found deployments in running/starting status, using the cloud provider parameter '" +
									toUnshare.getName() + "' shared with team '" + team.getName() + "'." );
							this.stopDeploymentByReference(deployment.getAccount().getUsername(), deployment.getReference());
							toNotify.add(deployment.getAccount().getEmail());
						}
					}catch(Exception e){
						logger.error("Failed to stop deployment, on unsharing cloud credential");
					}
				}
			});

			if(!toNotify.isEmpty()){
				logger.info("There are users who are to be notified, regarding deployments destruction");
				String message = "Your deployments were destroyed. \n"
						+ "This was because the cloud credential " + "'" + toUnshare.name + "'" +" was \n"
						+ "unshared with you by " + toUnshare.getAccount().givenName + ".";
				try{
					SendMail.send(toNotify, "Deployments destroyed", message );
				}catch(IOException e){
					logger.error("Failed to send, deployments destroyed notification to the members of the team "
							+ "from which cloud credential '" + toUnshare.getName() + "' was unshared" );
				}
			}
		}

	}

	public void stopDeploymentsUsingGivenTeamSharedConfigurationDeploymentParameter(Team team,
			DeploymentService deploymentService, 
			DeploymentConfigurationService deploymentConfigurationService,
			ConfigurationDeploymentParameters toUnshare,
			ConfigurationService configurationService) {

		logger.info("Stopping deployments of team members(not owner's) on unsharing a configuration deployment parameter.");
		List<String> toNotify = new ArrayList<>();
		logger.info("Get member accounts of team " + team.getName());
		Set<Account> memberAccounts = team.getAccountsBelongingToTeam();
		if (team.getConfigDepParamsBelongingToTeam().stream().map(cdp -> cdp.getId()).collect(Collectors.toList()).contains(toUnshare.getId())) {
			logger.info("Find all deployments using the configuration to be unshared");
			//now check for deployments with the configuration reference
			List<Configuration> configurations = configurationService.findAll().stream()
					.filter(configuration -> configuration.getConfigDeployParamsReference().equals(toUnshare.getReference()))
					.collect(Collectors.toList());
			configurations.forEach(configuration -> {
				List<DeploymentConfiguration> deploymentConfiguration = deploymentConfigurationService.findAll()
						.stream().filter(dc -> (dc.getConfigurationReference() != null) && dc.getConfigurationReference().equals(configuration.getReference())).collect(Collectors.toList());
				deploymentConfiguration.forEach(dc -> {
					Deployment deployment = dc.getDeployment();
					if(deployment.getAccount().getId()!= toUnshare.getAccount().getId() 
							&& memberAccounts.stream().map(account -> account.getId()).collect(Collectors.toList()).contains(deployment.getAccount().getId())){
						try{
							if(deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.RUNNING)
									|| deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.STARTING)){
								logger.info("Found deployments in running/starting status, using the configuration '" +
										configuration.getName() + "' shared with team '" + team.getName() + "'." );
								this.stopDeploymentByReference(deployment.getAccount().getUsername(), deployment.getReference());
								toNotify.add(deployment.getAccount().getEmail());
							}

						}catch(Exception e){
							logger.error("Failed to stop deployment, on unsharing configuration deployment parameters");
						}
					}
				});
			});

			if(!toNotify.isEmpty()){
				logger.info("There are users who are to be notified, regarding deployments destruction");
				String message = "Your deployments were destroyed. \n"
						+ "This was because the configuration deployment parameters " + "'" + toUnshare.name + "'" + " was \n"
						+ "unshared with you by " + toUnshare.getAccount().givenName + ".";
				try{
					SendMail.send(toNotify, "Deployments destroyed", message );
				}catch(IOException e){
					logger.error("Failed to send, deployments destroyed notification to the members of the team "
							+ "from which configuration deployment parameter'" + toUnshare.getName() + "' was unshared" );
				}
			}
		}
	}

	public void stopDeploymentsUsingGivenTeamSharedConfiguration(Team team, 
			DeploymentService deploymentService, 
			DeploymentConfigurationService deploymentConfigurationService,
			Configuration toUnshare) {

		logger.info("Stopping deployments of team members(not owner's) on unsharing a configuration.");
		List<String> toNotify = new ArrayList<>();
		logger.info("Getting member accounts of team " + team.getName());
		Set<Account> memberAccounts = team.getAccountsBelongingToTeam();
		if (team.getConfigurationsBelongingToTeam().contains(toUnshare)) {

			logger.info("Getting deployments which uses the configuration to be unshared");
			//now remove deployments having this configuration
			List<DeploymentConfiguration> deploymentConfiguration = deploymentConfigurationService.findAll()
					.stream().filter(dc -> (dc.getConfigurationReference() != null) && 
							dc.getConfigurationReference().equals(toUnshare.getReference())
							).collect(Collectors.toList());

			deploymentConfiguration.forEach(dc -> {
				Deployment deployment = dc.getDeployment();
				if(deployment.getAccount().getId() != toUnshare.getAccount().getId()
						&& memberAccounts.stream().map(account -> account.getId()).collect(Collectors.toList()).contains(deployment.getAccount().getId())){
					try{
						if(deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.RUNNING)
								|| deployment.deploymentStatus.getStatus().equals(DeploymentStatusEnum.STARTING)){
							logger.info("Found deployments in running/starting status, using the configuration '" +
									toUnshare.getName() + "' shared with team '" + team.getName() + "'." );
							this.stopDeploymentByReference(deployment.getAccount().getUsername(), deployment.getReference());
							toNotify.add(deployment.getAccount().getEmail());
						}
					}catch(Exception e){
						logger.error("Failed to stop deployment, on unsharing configuration");
					}
				}
			});
			if(!toNotify.isEmpty()){
				logger.info("There are users who are to be notified, regarding deployments destruction");
				String message = "Your deployments were destroyed. \n"
						+ "This was because the configuration " + "'" + toUnshare.name + "'" +" was \n"
						+ "unshared with you by " + toUnshare.getAccount().givenName + ".";
				try{
					SendMail.send(toNotify, "Deployments destroyed", message );
				}catch(IOException e){
					logger.error("Failed to send, deployments destroyed notification to the members of the team "
							+ "from which configuration deployment parameter'" + toUnshare.getName() + "' was unshared" );
				}
			}

		}

	}

	public void leaveTeam(
			String token,
			DeploymentService deploymentService, 
			ConfigurationService configurationService, 
			ConfigurationDeploymentParametersService configDepParamsService, 
			TeamResource teamResource) throws Exception {

		try{

			Team team = this.findByName(teamResource.getName());

			Set<String> memberAccountEmails = team.getAccountsBelongingToTeam().stream().
					map(Account::getEmail).
					collect(Collectors.toSet());

			String memberToLeaveEmail = (String) teamResource.getMemberAccountEmails().toArray()[0];
			List<String> toNotify = new ArrayList();
			toNotify.add(memberToLeaveEmail);

			if(memberAccountEmails.contains(memberToLeaveEmail)){
				logger.info("Member is a part of the team");
				if(team.getAccount().getEmail().equals(memberToLeaveEmail)){
					logger.info("Team owner wants to leave team");
					throw new Exception("Team owner cannot leave the team, but he can delete the team");
				}else{
					logger.info("Removing member from team");
					this.removeMemberFromTeam(
							token,
							team.getName(), 
							memberToLeaveEmail);
					try {
						this.stopDeploymentsOfRemovedUser(team,
									memberToLeaveEmail,
									deploymentService,
									configurationService, 
									configDepParamsService
									);
                        String message = "You have been removed from the team " + "'"+team.getName()+"'" + ".\n\n";
                        SendMail.send(toNotify, "Request to leave team " + team.getName(), message );
					}catch(IOException e){
						logger.error("In leaveTeam: Failed to notify user");
					}
				}
			}else{
				throw new Exception("User has to be a member of the team, to leave it");
			}
		}catch(TeamNotFoundException e){
			logger.error("In leaveTeam: Could not find team '"+teamResource.getName()+"'.");
			throw e;
		}

	}

	public void addMemberOnRequest(
			String baseURL,
			TeamResource teamResource) throws IOException {

		Team team = this.findByName(teamResource.getName());

		Set<String> memberAccountEmails = team.getAccountsBelongingToTeam().stream().map(Account::getEmail).collect(Collectors.toSet());

		Collection<String> yetToBeMemberEmails = teamResource.getMemberAccountEmails().stream().filter(
				a -> !memberAccountEmails.contains(a)).collect(Collectors.toSet());
		Account yetToBeMember = yetToBeMemberEmails.stream().map(
				email -> accountService.findByEmail(email)).collect(Collectors.toList()).get(0);

		List<String> toNotify = new ArrayList();
		toNotify.add(team.getAccount().getEmail());

		String loginURL = baseURL + "login";
		String teamURL = baseURL + "team" + "/" + team.getName() ;

		String message = "User " + yetToBeMember.givenName + 
				" would like to join team " + "'"+team.getName()+"'" + ".\n\n"
				+ "Please click on the link below to login if you haven't already \n" 
				+ loginURL.replaceAll(" ", "%20") + "\n\nor click on " + teamURL.replaceAll(" ", "%20")
				+ "\n"
				+ "and use the email " + yetToBeMember.getEmail() + " to add member.\n\n";

		SendMail.send(toNotify, "Request to join team " + team.getName(), message );

	}

	public ResponseEntity<?> stopDeploymentByReference(
			String userName,
			String reference) throws IOException, ApplicationDeployerException {

		logger.info("Stopping deployment '" + reference + "'");

		Account account = this.accountService.findByUsername(userName);
		Deployment theDeployment = this.deploymentService.findByReference(reference);

		// get credentials decrypted through the service layer
		CloudProviderParamsCopy theCloudProviderParametersCopy;
		theCloudProviderParametersCopy = this.cloudProviderParametersCopyService.findByCloudProviderParametersReference(theDeployment.getCloudProviderParametersReference());

		DeploymentConfiguration deploymentConfiguration = null;
		if(theDeployment.getDeploymentConfiguration() != null){
			deploymentConfiguration = this.deploymentConfigurationService.findByDeployment(theDeployment);
		}

		// Update status
		theDeployment.getDeploymentStatus().setStatus(DeploymentStatusEnum.DESTROYING);
		this.deploymentService.save(theDeployment);

		// Proceed to destroy
		this.applicationDeployerBash.destroy(
				theDeployment.getDeploymentApplication().getRepoPath(),
				theDeployment.getReference(),
				this.getCloudProviderPathFromDeploymentApplication(
						theDeployment.getDeploymentApplication(), theCloudProviderParametersCopy.getCloudProvider()),
				theDeployment.getAssignedInputs(),
				theDeployment.getAssignedParameters(),
				theDeployment.getAttachedVolumes(),
				deploymentConfiguration,
				theCloudProviderParametersCopy
				);

		// Prepare response
		HttpHeaders httpHeaders = new HttpHeaders();

		return new ResponseEntity<>(null, httpHeaders, HttpStatus.OK);
	}

	public static String getCloudProviderPathFromDeploymentApplication(DeploymentApplication deploymentApplication, String cloudProvider) {

		logger.info("Getting the path of the cloud provider from deploymentApplication");
		Iterator<DeploymentApplicationCloudProvider> it = deploymentApplication.getCloudProviders().iterator();

		while ( it.hasNext() ) {
			DeploymentApplicationCloudProvider cp = it.next();
			if (cloudProvider.equals(cp.getName())) {
				return cp.getPath();
			}
		}

		return null;
	}

}
