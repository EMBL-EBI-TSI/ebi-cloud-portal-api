package uk.ac.ebi.tsc.portal.api.application.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.TokenService;
import uk.ac.ebi.tsc.aap.client.security.TokenHandler;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.service.AccountService;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.application.service.ApplicationNotFoundException;
import uk.ac.ebi.tsc.portal.api.application.service.ApplicationService;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentApplication;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentApplicationService;
import uk.ac.ebi.tsc.portal.api.team.repo.TeamRepository;
import uk.ac.ebi.tsc.portal.api.team.service.TeamService;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDownloader;
import uk.ac.ebi.tsc.portal.clouddeployment.exceptions.ApplicationDownloaderException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@RestController
@RequestMapping(value = "/application", produces = {MediaType.APPLICATION_JSON_VALUE})
public class ApplicationRestController {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationRestController.class);

	@Value("${be.applications.root}")
	private String applicationsRoot;

	private final ApplicationService applicationService;

	private final AccountService accountService;

	private ApplicationDownloader applicationDownloader;
	
	private uk.ac.ebi.tsc.aap.client.security.TokenHandler tokenHandler;
	
	private final DeploymentApplicationService deploymentApplicationService;

	private final TeamService teamService;

	private final TokenService tokenService;

	@Autowired
	ApplicationRestController(
			ApplicationService applicationService,
			AccountService accountService,
			ApplicationDownloader applicationDownloader,
			TeamRepository teamRepository,
			TokenHandler  tokenHandler,
			DomainService domainService,
			DeploymentApplicationService deploymentApplicationService,
			TeamService teamService,
			TokenService tokenService,
			ResourceLoader resourceLoader,
			@Value("${ecp.aap.username}") final String ecpAapUsername,
			@Value("${ecp.aap.password}") final String ecpAapPassword,
			@Value("${ecp.default.teams.file}") final String ecpDefaultTeamsFilePath) {
		this.applicationService = applicationService;
		this.accountService = accountService;
		this.applicationDownloader = applicationDownloader;
		this.tokenHandler = tokenHandler;
		this.deploymentApplicationService = deploymentApplicationService;
		this.teamService = teamService;
		this.tokenService = tokenService;
	}

	/* useful to inject values without involving spring - i.e. tests */
	void setProperties(Properties properties) {
		this.applicationsRoot = properties.getProperty("be.applications.root");
	}

	@RequestMapping(method = RequestMethod.POST, value = "/addToDefaultTeam")
	public void addAccountToDefaultTeamsByEmail(Principal principal) throws IOException, ApplicationDownloaderException {
		logger.info("In ApplicationService: Adding to default team");
		Account account = accountService.findByUsername(principal.getName());
		this.teamService.addAccountToDefaultTeamsByEmail(account);
	}

	@RequestMapping(method = RequestMethod.POST)
	ResponseEntity<?> add(Principal principal, @RequestBody ApplicationResource input) throws IOException, ApplicationDownloaderException {

		logger.info("Adding new application from " + input.getRepoUri());

		try {
			this.applicationService.findByAccountUsernameAndName(principal.getName(), input.getName());
			throw new ApplicationDownloaderException("Application from " + input.getRepoUri() + " already exists");
		} catch (ApplicationNotFoundException ane) {
			Application application = applicationDownloader.downloadApplication(
					this.applicationsRoot, input.getRepoUri(), principal.getName()
					);

			Application newApplication = this.applicationService.save(
					application
					);
			// Prepare response
			HttpHeaders httpHeaders = new HttpHeaders();

			Link forOneApplication = new ApplicationResource(newApplication).getLink("self");
			httpHeaders.setLocation(URI.create(forOneApplication.getHref()));

			ApplicationResource applicationResource = new ApplicationResource(newApplication);

			return new ResponseEntity<>(applicationResource, httpHeaders, HttpStatus.CREATED);
		}
	}

	@RequestMapping(method = RequestMethod.GET)
	public Resources<ApplicationResource> getAllApplications(Principal principal, Sort sort) {
		logger.info("Application list requested by user " + principal.getName());

		Account account = this.accountService.findByUsername(principal.getName());
		this.teamService.addAccountToDefaultTeamsByEmail(account);
		List<ApplicationResource> applicationResourceList =
				this.applicationService.findByAccountUsername(account.getUsername(), sort)
				.stream()
				.map(ApplicationResource::new)
				.collect(Collectors.toList());
		logger.info("Returning the list of all applications");
		return new Resources<>(applicationResourceList);

	}

	@RequestMapping(value = "/{name:.+}", method = RequestMethod.GET)
	public ApplicationResource getApplicationByAccountUsernameAndName(Principal principal, @PathVariable("name") String name) {

		  logger.info("Application " + name + " requested by user " + principal.getName());

	      return new ApplicationResource(this.applicationService.findByAccountUsernameAndName(principal.getName(), name));
	}

	@RequestMapping(value = "/{name:.+}", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteApplicationByAccountUsernameAndName(Principal principal, @PathVariable("name") String name) throws IOException, ApplicationDownloaderException {
		logger.info("Application " + name + " deletion requested by user " + principal.getName());

		// delete from DB
		Application application = this.applicationService.findByAccountUsernameAndName(principal.getName(), name);
		this.applicationService.delete(application.getId());

		List<DeploymentApplication> deployedApplications = 
		this.deploymentApplicationService.findByAccountIdAndRepoPath(application.getAccount().getId(), application.getRepoPath());
		

		// delete git repo if there are no deployments
		
		if(deployedApplications.isEmpty()){
			logger.info("There are no deployments associated, with application,therefore removing the repo");
			applicationDownloader.removeApplication(application.getRepoPath(), application.getRepoUri());
		}

		
		// Prepare response
		HttpHeaders httpHeaders = new HttpHeaders();

		return new ResponseEntity<>(null, httpHeaders, HttpStatus.OK);

	}


	@RequestMapping(value = "/shared", method = {RequestMethod.GET})
	public Resources<ApplicationResource> getSharedApplicationByAccount(HttpServletRequest request, Principal principal) {

		logger.info("List of shared applications of account requested " + principal.getName() + "  requested");
		Account account = this.accountService.findByUsername(principal.getName());
		String token = request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
		List<ApplicationResource> applicationsResourceList = 
				 applicationService.getSharedApplicationsByAccount(account, token, tokenHandler.parseUserFromToken(token))
				.stream()
				.map(ApplicationResource::new)
				.collect(Collectors.toList());

		return new Resources<>(applicationsResourceList);

	}

	@RequestMapping(value = "/shared/{name:.+}", method = {RequestMethod.GET})
	public ApplicationResource getSharedByName(HttpServletRequest request, Principal principal, @PathVariable("name") String name) {

		logger.info("Account " + principal.getName() + " requested shared application " + name);

		Account account = this.accountService.findByUsername(principal.getName());
		String token = request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
		
		Application application = applicationService.getSharedApplicationByApplicationName(account, token, tokenHandler.parseUserFromToken(token), name);

		if (application!=null) {
			return new ApplicationResource(
					this.applicationService.findByAccountUsernameAndName(application.getAccount().getUsername(),name)
					);
		} else {
			throw new ApplicationNotFoundException(name);
		}

	}

}
