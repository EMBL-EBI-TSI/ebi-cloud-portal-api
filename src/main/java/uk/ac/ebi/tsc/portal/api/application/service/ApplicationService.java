package uk.ac.ebi.tsc.portal.api.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationRepository;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Service
public class ApplicationService {

	private final ApplicationRepository applicationRepository;
	private final DomainService domainService;

	private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);

	@Autowired
	public ApplicationService(ApplicationRepository applicationRepository, DomainService domainService) {
		this.applicationRepository = applicationRepository;
		this.domainService = domainService;

	}


	public Application save(Application application) {
		return this.applicationRepository.save(application);
	}

	public Collection<Application> findByAccountUsername(String username, Sort sort) {
		return applicationRepository.findByAccountUsername(username, sort);
	}

	public Application findByAccountUsernameAndName(String username, String name) {
		return applicationRepository.findByAccountUsernameAndName(username, name).orElseThrow(
				() -> new ApplicationNotFoundException(name));
	}

	public Application findById(Long applicationId) {
		return this.applicationRepository.findById(applicationId).orElseThrow(
				() -> new ApplicationNotFoundException(applicationId));
	}

	public Application findByReference(String reference) {
		return this.applicationRepository.findByReference(reference).orElseThrow(
				() -> new ApplicationNotFoundException(reference));
	}

	public void delete(Long applicationId) {
		this.applicationRepository.findById(applicationId).orElseThrow(
				() -> new ApplicationNotFoundException(applicationId));
		this.applicationRepository.delete(applicationId);
	}

	public Set<Application> getSharedApplicationsByAccount(Account account,  String token, User user ){

		Set<Application> sharedApplications = new HashSet<>();
		logger.info("In ApplicationService: getting shared applications");

		for(Team memberTeam: account.getMemberOfTeams()){
			try{
				logger.info("In ApplicationService: checking if team has a domain reference ");
				if(memberTeam.getDomainReference() != null){
					logger.info("In ApplicationService: check if user is a domain member");
					Domain domain = domainService.getDomainByReference(memberTeam.getDomainReference(), token);
					if(domain != null){
						Collection<User> users = domainService.getAllUsersFromDomain(domain.getDomainReference(), token);
						User domainUser = users.stream().filter(u -> u.getEmail().equals(user.getEmail())).findAny().orElse(null);
						if(domainUser != null){
							logger.info("In ApplicationService: returning shared applications , if user is member of team and domain");
							sharedApplications.addAll(memberTeam.getApplicationsBelongingToTeam());
						}
					}
				}else{
					logger.info("In ApplicationService:  returning shared application, if user is member of team and no domain present");
					sharedApplications.addAll(memberTeam.getApplicationsBelongingToTeam());
				}
			}catch(Exception e){
				logger.error("Could not add all shared applications from team " + memberTeam.getName());
			}
		}
		//print the shared application info
		for(Application a: sharedApplications){
			logger.info("Application name: " + a.getName() + "from repo " + a.getRepoPath());
		}
		return sharedApplications;
	}

	public Application getSharedApplicationByApplicationName(Account account,  String token, User user, String applicationName ){

		logger.info("In ApplicationService: getting shared applications by name");
		for(Team memberOfTeam: account.getMemberOfTeams()){
			try{
				logger.info("In ApplicationService: checking if team has a domain reference ");
				if(memberOfTeam.getDomainReference() != null){
					logger.info("In ApplicationService: check if user is a domain member");
					Domain domain = domainService.getDomainByReference(memberOfTeam.getDomainReference(), token);
					if(domain != null){
						Collection<User> users = domainService.getAllUsersFromDomain(domain.getDomainReference(), token);
						User domainUser = users.stream().filter(u -> u.getEmail().equals(user.getEmail())).findAny().orElse(null);
						if(domainUser != null){
							logger.info("In ApplicationService: returning the shared application , if user is member of team and domain");
							return memberOfTeam.getApplicationsBelongingToTeam().stream().filter(app -> applicationName.equals(app.getName()))
									.findFirst().get();
						}
					}
				}else{
					logger.info("In ApplicationService:  returning shared application by name, if user is member of team and no domain present");
					return memberOfTeam.getApplicationsBelongingToTeam().stream().filter(app -> applicationName.equals(app.getName()))
							.findFirst().get();
				}
			}catch(Exception e){
				logger.error("Could not add shared application " + applicationName + e.getMessage() );
			}
		}
		return null;
	}

	public boolean isApplicationSharedWithAccount(Account account, Application application){
		if(account.getMemberOfTeams().stream().anyMatch(t ->
		t.getApplicationsBelongingToTeam().stream().anyMatch(a -> a.equals(application)))){
			return true;
		}
		return false;
	}
}
