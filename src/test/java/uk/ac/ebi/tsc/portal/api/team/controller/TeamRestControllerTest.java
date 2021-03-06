package uk.ac.ebi.tsc.portal.api.team.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.TokenService;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.service.AccountService;
import uk.ac.ebi.tsc.portal.api.application.controller.ApplicationResource;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.application.service.ApplicationService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.controller.CloudProviderParametersResource;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParametersService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyService;
import uk.ac.ebi.tsc.portal.api.configuration.controller.ConfigurationDeploymentParametersResource;
import uk.ac.ebi.tsc.portal.api.configuration.controller.ConfigurationResource;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationDeploymentParametersService;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationService;
import uk.ac.ebi.tsc.portal.api.deployment.repo.Deployment;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentConfiguration;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentConfigurationService;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.encryptdecrypt.security.EncryptionService;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;
import uk.ac.ebi.tsc.portal.api.team.repo.TeamRepository;
import uk.ac.ebi.tsc.portal.api.team.service.*;
import uk.ac.ebi.tsc.portal.api.utils.SendMail;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDeployer;
import uk.ac.ebi.tsc.portal.clouddeployment.exceptions.ApplicationDeployerException;
import uk.ac.ebi.tsc.portal.security.DefaultTeamMap;

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
import java.sql.Date;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class TeamRestControllerTest {

	@MockBean
	private TeamRestController subject;

	@MockBean
	private TeamService teamService;

	@MockBean
	private ApplicationService applicationService;

	@MockBean
	private CloudProviderParametersService cppService;

	@MockBean
	private ConfigurationService configurationService;

	@MockBean
	private ConfigurationDeploymentParametersService configDepParamsService;

	@MockBean
	private AccountService accountService;

	@MockBean
	private DomainService domainService;

	@MockBean
	private Principal principal;
	private String principalName = "principalName";

	@MockBean
	private Account account;

	@MockBean
	private Team team;

	@MockBean
	private TeamResource teamResource ;
	String teamName = "teamName";

	@MockBean
	private ApplicationResource applicationResource;
	@MockBean
	private Application application;
	String applicationName = "applicationName";

	@MockBean
	private CloudProviderParametersResource cppResource;
	@MockBean
	private CloudProviderParameters cpp;
	String cppName = "cppName";

	@MockBean
	private ConfigurationResource configurationResource;
	@MockBean
	private Configuration configuration;
	String configurationName = "configurationName";

	@MockBean
	private ConfigurationDeploymentParametersResource configDepParamsResource;
	@MockBean
	private ConfigurationDeploymentParameters configDepParams;
	String configDepParamsName = "configDepParamsName";

	String userEmail = "userEmail";
	String someotherusername = "someotherusername";

	Account someAccount = mock(Account.class);
	Account someotherAccount = mock(Account.class);

	private Team toRemove;

	String tokenArray = "some token";
	String token = "token" ;

	@MockBean
	HttpServletRequest request;

	@MockBean
	HttpServletResponse response;

	@MockBean
	Domain domain;

	@MockBean
	DeploymentConfigurationService depConfigService;

	@MockBean
	DeploymentService deploymentService;

	@MockBean
	private CloudProviderParamsCopyService cppCopyService;

	@MockBean
	private EncryptionService encryptionService;

	@MockBean
	private SendMail sendMail;

	String baseURL = "something.api";

	@MockBean
	private ResourceLoader resourceLoader ;

	@MockBean
	private TeamRepository teamRepository;

	@MockBean
	private ApplicationDeployer applicationDeployer;

	@MockBean
	private TokenService tokenService;

	String defaultTeamsFile = "ecp.default.teams.file";

	private Map<String, List<DefaultTeamMap>> defaultTeamsMap = new HashMap<>();

	String ecpAapUsername = "ecpAapUsername";

	String ecpAapPassword = "ecpAapPassword";

	@Before
	public void setUp() throws IOException {
		ReflectionTestUtils.setField(teamService, "accountService", accountService);
		ReflectionTestUtils.setField(teamService, "domainService", domainService);
		ReflectionTestUtils.setField(teamService, "sendMail", sendMail);
		ReflectionTestUtils.setField(teamService, "ecpAapUsername", "ecpAapUsername");
		ReflectionTestUtils.setField(teamService, "ecpAapPassword", "ecpAapPassword");
		ReflectionTestUtils.setField(subject, "teamService", teamService);
		ReflectionTestUtils.setField(subject, "accountService", accountService);
		ReflectionTestUtils.setField(teamService, "accountService", accountService);
		ReflectionTestUtils.setField(teamService, "domainService", domainService);
		ReflectionTestUtils.setField(subject, "applicationService", applicationService);
		ReflectionTestUtils.setField(subject, "cloudProviderParametersService", cppService);
		ReflectionTestUtils.setField(subject, "configurationService", configurationService );
		ReflectionTestUtils.setField(subject, "configDepParamsService", configDepParamsService );
		ReflectionTestUtils.setField(subject, "deploymentConfigurationService", depConfigService );
		ReflectionTestUtils.setField(subject, "deploymentService", deploymentService);
		ReflectionTestUtils.setField(subject, "cloudProviderParametersCopyService", cppCopyService);
		ReflectionTestUtils.setField(cppCopyService, "encryptionService", encryptionService);
		ReflectionTestUtils.setField(cppService, "encryptionService", encryptionService);
		ReflectionTestUtils.setField(teamService, "sendMail", sendMail);
		ReflectionTestUtils.setField(subject, "applicationService", applicationService);
		toRemove = mock(Team.class);
	}


	@Test(expected = NullPointerException.class)
	public void testGetAllTeamsWhenUserHasNoTeams(){
		getPrincipal();
		getAccount();
		getRequest();
		given(teamService.findByAccountUsername(principalName)).willReturn(null);
		given(subject.getAllTeamsForCurrentUser(principal)).willCallRealMethod();
		Resources<TeamResource> teamResource = subject.getAllTeamsForCurrentUser(principal);
		assertNull(teamResource);
	}

	@Test
	public void testGetAllTeamsWhenUserIsAMemberOfTeam(){
		getPrincipal();
		getAccount();
		getRequest();
		Set<Team> teams = new HashSet<>();teams.add(team);
		given(teamService.findByAccountUsername(principalName)).willReturn(teams);
		getTeamResoure(team);
		given(subject.getAllTeamsForCurrentUser(principal)).willCallRealMethod();
		Resources<TeamResource> teamResource = subject.getAllTeamsForCurrentUser(principal);

		assertTrue(teamResource != null);
		assertTrue(teamResource.getContent().size() == 1);
	}

	@Test
	public void testGetTeamByName(){
		getPrincipal();
		getAccount();
		getRequest();
		getTeamResoure(team);
		addAccountsToTeam();
		given(teamService.findByName(teamName)).willReturn(team);
		given(subject.getTeamByName(request, principal, teamName)).willCallRealMethod();
		given(teamService.setManagerUsernamesAndEmails(isA(TeamResource.class), isA(String.class))).willCallRealMethod();
		given(teamService.populateTeamContactEmails(isA(Team.class), isA(TeamResource.class), isA(String.class))).willReturn(teamResource);
		given(teamService.findByDomainReference(team.getDomainReference())).willReturn(team);
		given(teamService.populateTeamMemberEmails(isA(Team.class), isA(TeamResource.class), isA(String.class))).willCallRealMethod();
		TeamResource teamResource = subject.getTeamByName(request, principal, teamName);
		assertTrue(teamResource.getName().equals(teamName));
	}

	@Test(expected = TeamNameInvalidInputException.class )
	public void testGetTeamByInvalidName(){
		getPrincipal();
		getAccount();
		getRequest();
		given(subject.getTeamByName(request, principal, null)).willCallRealMethod();
		subject.getTeamByName(request, principal, null);
	}

	@Test
	public void createNewTeam(){
		getPrincipal();
		getAccount();
		getTeamResoure(team);
		getRequest();
		getDomain();
		Mockito.when(teamService.constructTeam(principalName, teamResource, accountService, token)).thenReturn(team);
		teamService.constructTeam(principalName, teamResource, accountService, token);
		Mockito.when(teamService.save(team)).thenReturn(team);
		given(subject.createNewTeam(request, response, principal, teamResource)).willCallRealMethod();
		ResponseEntity<?> newTeam  = subject.createNewTeam(request, response, principal, teamResource);
		assertTrue(newTeam.getStatusCode().equals(HttpStatus.OK));
	}

	@Test(expected = TeamNameInvalidInputException.class )
	public void createNewTeamInvalidName(){
		getPrincipal();
		getAccount();
		String teamName="ds.dsf-fsd_ sd";
		given(team.getAccount()).willReturn(account);
		given(team.getName()).willReturn(teamName);
		given(teamResource.getName()).willReturn(teamName);
		getRequest();
		given(subject.createNewTeam(request, response, principal, teamResource)).willCallRealMethod();
		subject.createNewTeam(request, response, principal, teamResource);
	}

	@Test(expected = TeamNotCreatedException.class)
	public void createNewTeamFail(){
		getPrincipal();
		getAccount();
		getTeamResoure(team);
		getRequest();
		getFailDomainNull();
		Mockito.when(teamService.constructTeam(principalName, teamResource, accountService, token)).thenCallRealMethod();
		teamService.constructTeam(principalName, teamResource, accountService, token);
		Mockito.when(teamService.save(team)).thenReturn(team);
		given(subject.createNewTeam(request, response, principal, teamResource)).willCallRealMethod();
		subject.createNewTeam(request, response, principal, teamResource);
	}

	@Test(expected = TeamNotCreatedException.class)
	public void createNewTeamException(){
		getPrincipal();
		getAccount();
		getTeamResoure(team);
		getRequest();
		getFailDomainException();
		Mockito.when(teamService.constructTeam(principalName, teamResource, accountService, token)).thenCallRealMethod();
		teamService.constructTeam(principalName, teamResource, accountService, token);
		Mockito.when(teamService.save(team)).thenReturn(team);
		given(subject.createNewTeam(request, response, principal, teamResource)).willCallRealMethod();
		subject.createNewTeam(request, response, principal, teamResource);
	}

	@Test(expected = TeamNameInvalidInputException.class )
	public void createNewTeamInvalidTeamName(){
		getPrincipal();
		getAccount();
		String emptyTeamName = "";
		given(team.getAccount()).willReturn(account);
		given(team.getName()).willReturn(emptyTeamName);
		getRequest();
		getDomain();
		Mockito.when(teamService.constructTeam(principalName, teamResource, accountService, token)).thenReturn(team);
		teamService.constructTeam(principalName, teamResource, accountService, token);
		given(subject.createNewTeam(request, response, principal, teamResource)).willCallRealMethod();
		subject.createNewTeam(request, response, principal, teamResource);
	}

	@Test
	public void testDeleteTeamPass(){
		getRequest();
		given(principal.getName()).willReturn(principalName);
		given(teamService.findByNameAndAccountUsername(teamName, principalName)).willReturn(team);

		given(subject.deleteTeam(request, principal, teamName)).willCallRealMethod();
		ResponseEntity<?> teamDeleted = subject.deleteTeam(request, principal, teamName);
		assertTrue(teamDeleted.getStatusCode().equals(HttpStatus.OK));
	}

	@Test(expected = TeamNameInvalidInputException.class )
	public void testDeleteTeamInvalidTeamName(){
		getRequest();
		given(subject.deleteTeam(request, principal, null)).willCallRealMethod();
		subject.deleteTeam(request, principal, null);
	}

	@Test
	public void addMemberToTeamPass() throws AccountNotFoundException {
		String someAccountEmail = userEmail;
		String someOtherAccountEmail =  "anEmail";
		getPrincipal();
		getTeamResoure(team);
		given(someAccount.getEmail()).willReturn(someAccountEmail);
		given(someotherAccount.getEmail()).willReturn(someOtherAccountEmail);
		given(someAccount.getUsername()).willReturn("someusername");
		given(someotherAccount.getEmail()).willReturn(someOtherAccountEmail );
		Set<Account> accounts = new HashSet<>();
		accounts.add(someAccount);
		accounts.add(someotherAccount);
		team.getAccountsBelongingToTeam().addAll(accounts);
		Mockito.when(team.getAccountsBelongingToTeam()).thenReturn(accounts);
		getDomain();
		getRequest();
		assertTrue(team.getAccountsBelongingToTeam().size() == 2);
		given(teamService.findByName(teamName)).willReturn(team);

        TeamResource teamResource = new TeamResource();
        teamResource.setName(teamName);
        Set<String> toBeMemberEmails = new HashSet<>();
        String accountToAddEmail = "accountToAddEmail";
        toBeMemberEmails.add(accountToAddEmail );
        teamResource.setMemberAccountEmails(toBeMemberEmails);

		Account toAddAccount = mock(Account.class);
		given(toAddAccount.getEmail()).willReturn(accountToAddEmail);
		given(accountService.findByEmail(accountToAddEmail)).willReturn(toAddAccount);

		Mockito.when(teamService.addMemberToTeam(token, teamResource.getName(), teamResource.getMemberAccountEmails(), null)).thenCallRealMethod();
		Mockito.when(domainService.getDomainByReference(team.getDomainReference(), token)).thenReturn(domain);
		Mockito.when(domainService.addUserToDomain(Mockito.any(Domain.class), Mockito.any(User.class), Mockito.anyString())).thenReturn(domain);
		Set<User> users = new HashSet<>();
		User user = mock(User.class);
		Mockito.when(user.getEmail()).thenReturn(accountToAddEmail);
		users.add(user);
		Mockito.when(domainService.getAllUsersFromDomain(Mockito.anyString(), Mockito.anyString())).thenReturn(users);
		given(accountService.save(toAddAccount)).willReturn(toAddAccount);
		//given(teamService.getBaseURL(Mockito.any(HttpServletRequest.class))).willReturn("something.api");
		given(teamService.save(team)).willReturn(team);
		given(subject.addMember(request, principal, teamResource)).willCallRealMethod();
		ResponseEntity<?> memberAdded = subject.addMember(request, principal, teamResource);
		assertTrue(team.getAccountsBelongingToTeam().size() == 3);
	}

	@Test
	public void addMemberToTeamOnRequestPass() throws IOException{
		String someAccountEmail = userEmail;
		String someOtherAccountEmail =  "anEmail";
		getPrincipal();
		getRequest();
		getTeamResoure(team);
		given(team.getAccount()).willReturn(account);
		given(account.getEmail()).willReturn("some email");
		given(someAccount.getEmail()).willReturn(someAccountEmail);
		given(someotherAccount.getEmail()).willReturn(someOtherAccountEmail);
		given(someAccount.getUsername()).willReturn("someusername");
		given(someotherAccount.getEmail()).willReturn(someOtherAccountEmail );
		Set<Account> accounts = new HashSet<>();
		accounts.add(someAccount);accounts.add(someotherAccount);
		team.getAccountsBelongingToTeam().addAll(accounts);
		Mockito.when(team.getAccountsBelongingToTeam()).thenReturn(accounts);
		getDomain();
		assertTrue(team.getAccountsBelongingToTeam().size() == 2);
		given(teamService.findByName(teamName)).willReturn(team);
        TeamResource teamResource = new TeamResource();
        teamResource.setName(teamName);
        Set<String> toBeMemberEmails = new HashSet<>();
        String accountToAddEmail = "accountToAddEmail";
        toBeMemberEmails.add(accountToAddEmail );
        teamResource.setMemberAccountEmails(toBeMemberEmails);

		Account toAddAccount = mock(Account.class);
		toBeMemberEmails.add(someOtherAccountEmail);
		toBeMemberEmails.add(someAccountEmail);
		given(toAddAccount.getEmail()).willReturn(accountToAddEmail);
		given(accountService.findByEmail(accountToAddEmail)).willReturn(toAddAccount);

		given(subject.addMemberOnRequest(request, principal, teamResource)).willCallRealMethod();
		ResponseEntity<?> memberAdded = subject.addMemberOnRequest(request, principal, teamResource);
		assertTrue(memberAdded.getStatusCode().equals(HttpStatus.OK));
		assertTrue(team.getAccountsBelongingToTeam().size() == 2);
	}

	@Test(expected = TeamMemberNotAddedException.class)
	public void addMemberToTeamFail() throws AccountNotFoundException {

		getPrincipal();
		getTeamResoure(team);
		addAccountsToTeam();
		getFailDomainNull();
		getRequest();
		assertTrue(team.getAccountsBelongingToTeam().size() == 2);
		given(teamService.findByName(teamName)).willReturn(team);

		TeamResource teamResource = new TeamResource();
		teamResource.setName(teamName);
		Set<String> toBeMemberEmails = new HashSet<>();
		String accountToAddEmail = "accountToAddEmail";
		toBeMemberEmails.add(accountToAddEmail );
		teamResource.setMemberAccountEmails(toBeMemberEmails);

		Account toAddAccount = mock(Account.class);
		given(toAddAccount.getEmail()).willReturn(accountToAddEmail);
		given(accountService.findByEmail(accountToAddEmail)).willReturn(toAddAccount);
		Mockito.when(teamService.addMemberToTeam(token, teamResource.getName(), teamResource.getMemberAccountEmails(), baseURL)).thenCallRealMethod();
		teamService.addMemberToTeam(token, teamResource.getName(), teamResource.getMemberAccountEmails(), baseURL);
		given(subject.addMember(request, principal, teamResource)).willCallRealMethod();
		assertTrue(team.getAccountsBelongingToTeam().size() == 2);
		subject.addMember(request, principal, teamResource);
	}

	@Test(expected = TeamMemberNotAddedException.class)
	public void addMemberToTeamFailNoDomain() throws AccountNotFoundException {

		String someAccountEmail = userEmail;
		String someOtherAccountEmail =  "anEmail";
		getPrincipal();

		given(someAccount.getEmail()).willReturn(someAccountEmail);
		given(someotherAccount.getEmail()).willReturn(someOtherAccountEmail);
		given(someAccount.getUsername()).willReturn("someusername");
		given(someotherAccount.getEmail()).willReturn(someOtherAccountEmail );
		Set<Account> accounts = new HashSet<>();
		accounts.add(someAccount);accounts.add(someotherAccount);
		team.getAccountsBelongingToTeam().addAll(accounts);
		Mockito.when(team.getAccountsBelongingToTeam()).thenReturn(accounts);
		getRequest();
		assertTrue(team.getAccountsBelongingToTeam().size() == 2);
		given(teamService.findByName(teamName)).willReturn(team);


		String accountToAddEmail = "accountToAddEmail";

		Account toAddAccount = mock(Account.class);
		given(toAddAccount.getEmail()).willReturn(accountToAddEmail);
		given(accountService.findByEmail(accountToAddEmail)).willReturn(null);

        TeamResource teamResource = new TeamResource();
        teamResource.setName(teamName);
        Set<String> toBeMemberEmails = new HashSet<>();
        toBeMemberEmails.add(accountToAddEmail );
        toBeMemberEmails.add(someOtherAccountEmail);
        toBeMemberEmails.add(someAccountEmail);
		teamResource.setMemberAccountEmails(toBeMemberEmails);

		Mockito.when(teamService.addMemberToTeam(token, teamResource.getName(), teamResource.getMemberAccountEmails(), null)).thenCallRealMethod();
		given(subject.addMember(request, principal, teamResource)).willCallRealMethod();
		ResponseEntity<?> memberAdded = subject.addMember(request, principal, teamResource);
		assertTrue(memberAdded.getStatusCode().equals(HttpStatus.OK));
		assertTrue(team.getAccountsBelongingToTeam().size() == 2);
	}

	@Test(expected = TeamNameInvalidInputException.class )
	public void addMemberToTeamInvalidTeamName() throws AccountNotFoundException {

		getPrincipal();
		given(teamResource.getName()).willReturn(null);
		given(subject.addMember(request, principal, teamResource)).willCallRealMethod();
		subject.addMember(request, principal, teamResource);
	}

	@Test
	public void testRemoveMemberFromTeamPass() throws Exception {

		getPrincipal();
		addAccountsToTeam();
		getMemberOfTeams();
		getTeamResoure(team);
		getDomain();
		getRequest();
		assertTrue(someotherAccount.getMemberOfTeams().size() == 2);
		assertTrue(team.getAccountsBelongingToTeam().size() == 2);
		given(accountService.findByEmail(userEmail)).willReturn(someotherAccount);
		given(teamService.findByName(teamName)).willReturn(team);
		Mockito.when(teamService.save(team)).thenReturn(team);
		Mockito.when(domainService.getDomainByReference(team.getDomainReference(), token )).thenReturn(domain);
		Mockito.when(someotherAccount.getEmail()).thenReturn(userEmail);
		Mockito.when(someotherAccount.getUsername()).thenReturn(someotherusername);
		Mockito.when(domainService.removeUserFromDomain(Mockito.any(User.class), Mockito.any(Domain.class), Mockito.any(String.class))).thenReturn(domain);
		given(subject.removeMemberFromTeam(request, principal, teamName, userEmail)).willCallRealMethod();
		Mockito.when(teamService.removeMemberFromTeam(token, team.getName(), userEmail)).thenCallRealMethod();
		ResponseEntity<?> memberDeleted = subject.removeMemberFromTeam(request, principal, teamName, userEmail);
		assertTrue(memberDeleted.getStatusCode().equals(HttpStatus.OK));
		assertTrue(team.getAccountsBelongingToTeam().size() == 1);
		assertTrue(someotherAccount.getMemberOfTeams().size() == 1);
	}

	@Test
	public void testRemoveMemberFromTeamNoDoaminPass() throws Exception {

		getPrincipal();
		addAccountsToTeam();
		getMemberOfTeams();
		getTeamResoureNoDomain(team);
		getRequest();
		assertTrue(someotherAccount.getMemberOfTeams().size() == 2);
		assertTrue(team.getAccountsBelongingToTeam().size() == 2);
		given(accountService.findByEmail(userEmail)).willReturn(someotherAccount);
		given(teamService.findByName(teamName)).willReturn(team);
		Mockito.when(teamService.save(team)).thenReturn(team);
		Mockito.when(someotherAccount.getEmail()).thenReturn(userEmail);
		Mockito.when(someotherAccount.getUsername()).thenReturn(someotherusername);
		given(subject.removeMemberFromTeam(request, principal, teamName, userEmail)).willCallRealMethod();
		Mockito.when(teamService.removeMemberFromTeam(token, team.getName(), userEmail)).thenCallRealMethod();
		ResponseEntity<?> memberDeleted = subject.removeMemberFromTeam(request, principal, teamName, userEmail);
		assertTrue(memberDeleted.getStatusCode().equals(HttpStatus.OK));
		assertTrue(team.getAccountsBelongingToTeam().size() == 1);
		assertTrue(someotherAccount.getMemberOfTeams().size() == 1);
	}

	@Test(expected = AccountNotFoundException.class)
	public void testRemoveMemberFromTeamNoDomainFail() throws Exception {

		getPrincipal();
		addAccountsToTeam();
		getMemberOfTeams();
		getTeamResoureNoDomain(team);
		getRequest();
		assertTrue(someotherAccount.getMemberOfTeams().size() == 2);
		assertTrue(team.getAccountsBelongingToTeam().size() == 2);
		given(accountService.findByEmail(userEmail)).willReturn(null);
		given(teamService.findByName(teamName)).willReturn(team);
		Mockito.when(teamService.save(team)).thenReturn(team);
		Mockito.when(someotherAccount.getEmail()).thenReturn(userEmail);
		Mockito.when(someotherAccount.getUsername()).thenReturn(someotherusername);given(subject.removeMemberFromTeam(request, principal, teamName, userEmail)).willCallRealMethod();
		Mockito.when(teamService.removeMemberFromTeam(token, team.getName(), userEmail)).thenCallRealMethod();
		ResponseEntity<?> memberDeleted = subject.removeMemberFromTeam(request, principal, teamName, userEmail);
		assertTrue(team.getAccountsBelongingToTeam().size() == 2);
		assertTrue(someotherAccount.getMemberOfTeams().size() == 2);
	}

	@Test
	public void testAddApplicationToTeam(){
		getPrincipal();
		getAccount();
		getApplicationResoure();
		getApplicationsBelongingTeam();
		getTeamResoure(team);
		assertTrue(team.getApplicationsBelongingToTeam().size() == 0);
		assertTrue(application.getSharedWithTeams().size() == 0);
		given(teamService.findByNameAndAccountUsername(Mockito.anyString(), Mockito.anyString())).willReturn(team);
		given(teamService.save(team)).willReturn(team);
		given(applicationService.findByAccountUsernameAndName(principalName, applicationName)).willReturn(application);
		given(subject.addApplicationToTeam(principal, applicationResource, teamName)).willCallRealMethod();
		ResponseEntity<?> applicationAdded = subject.addApplicationToTeam(principal, applicationResource, teamName);
		assertTrue(applicationAdded.getStatusCode().equals(HttpStatus.OK));
		assertTrue(team.getApplicationsBelongingToTeam().size() == 1);
		assertTrue(application.getSharedWithTeams().size() == 1);
	}

	@Test
	public void testGetAllApplications(){
		getPrincipal();
		getAccount();
		getApplicationResoure();
		given(application.getAccount()).willReturn(account);
		given(account.getEmail()).willReturn("anEmail");
		Set<Application> applications = new HashSet<>();
		applications.add(application);
		given(application.getRepoUri()).willReturn("repoUri");
		given(team.getApplicationsBelongingToTeam()).willReturn(applications);
		given(teamService.findByName(teamName)).willReturn(team);
		given(subject.getAllTeamApplications(principal, teamName)).willCallRealMethod();
		Resources<ApplicationResource> applicationResourceList =
				subject.getAllTeamApplications(principal, teamName);
		assertNotNull(applicationResourceList);
		assertTrue(applicationResourceList.getContent().size() == 1);
	}

	@Test
	public void testRemoveApplicationFromTeam(){
		Set<Application> applications = new HashSet<>();
		applications.add(application);
		Application toRemove = mock(Application.class);
		String name = "toRemove";
		given(toRemove.getName()).willReturn(name);
		applications.add(application);applications.add(toRemove);
		given(team.getApplicationsBelongingToTeam()).willReturn(applications);
		assertTrue(team.getApplicationsBelongingToTeam().size() == 2);
		given(applicationService.findByAccountUsernameAndName(principal.getName(), name))
		.willReturn(toRemove);
		given(teamService.findByNameAndAccountUsername(teamName, principal.getName())).willReturn(team);
		given(subject.removeApplicationFromTeam(principal, teamName, name)).willCallRealMethod();
		subject.removeApplicationFromTeam(principal, teamName, name);
		assertTrue(team.getApplicationsBelongingToTeam().size() == 1);
	}

	@Test
	public void testAddCppToTeam() throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeySpecException, BadPaddingException, IllegalBlockSizeException{
		getPrincipal();
		getAccount();
		getCppResoure();
		getCppBelongingTeam();
		getTeamResoure(team);
		assertTrue(team.getCppBelongingToTeam().size() == 0);
		assertTrue(cpp.getSharedWithTeams().size() == 0);
		given(cpp.getAccount()).willReturn(account);
		given(teamService.findByNameAndAccountUsername(Mockito.anyString(), Mockito.anyString() )).willReturn(team);
		given(teamService.save(team)).willReturn(team);
		given(cppService.findByNameAndAccountUsername(cppName, principalName)).willReturn(cpp);
		given(subject.addCloudProviderParametersToTeam(principal, cppResource, teamName)).willCallRealMethod();
		ResponseEntity<?> cppAdded = subject.addCloudProviderParametersToTeam(principal, cppResource, teamName);
		assertTrue(cppAdded.getStatusCode().equals(HttpStatus.OK));
		assertTrue(team.getCppBelongingToTeam().size() == 1);
		assertTrue(cpp.getSharedWithTeams().size() == 1);
	}

	@Test
	public void testRemoveCppFromTeam() throws InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException, IOException{

		getPrincipal();
		given(principal.getName()).willReturn(principalName);
		Set<CloudProviderParameters> cpps = new HashSet<>();
		CloudProviderParameters toRemove = mock(CloudProviderParameters.class);
		String name = "toRemove";
		given(toRemove.getName()).willReturn(name);
		cpps.add(toRemove);
		given(toRemove.getAccount()).willReturn(account);
		given(team.getCppBelongingToTeam()).willReturn(cpps);
		team.setCppBelongingToTeam(team.getCppBelongingToTeam());
		Account account = mock(Account.class);
		given(account.getEmail()).willReturn("some email");
		Set<Account> accounts = new HashSet<>();
		accounts.add(account);
		team.accountsBelongingToTeam = accounts;
		given(team.getAccountsBelongingToTeam()).willReturn(accounts);
		Set<Team> teams = new HashSet<>();
		teams.add(team);
		given(toRemove.getSharedWithTeams()).willReturn(teams);
		given(toRemove.getId()).willReturn(1L);
		assertTrue(toRemove.getSharedWithTeams().size() == 1);
		assertTrue(team.getCppBelongingToTeam().size() == 1);
		given(cppService.findByNameAndAccountUsername(name, principalName)).willReturn(toRemove);
		given(toRemove.getAccount().getUsername()).willReturn(principalName);
		given(team.getAccount()).willReturn(account);
		given(team.getAccount().getUsername()).willReturn(principalName);
		given(teamService.findByNameAndAccountUsername(teamName, principalName)).willReturn(team);
		given(teamService.save(team)).willReturn(team);

		//set deployments
		List<Deployment> deployments = new ArrayList<>();
		Deployment deployment = mock(Deployment.class);
		String deploymentReference = "some_ref";
		given(deployment.getReference()).willReturn(deploymentReference);
		deployments.add(deployment);
		given(deploymentService.findAll()).willReturn(deployments);

		given(subject.removeCloudProviderParametersFromTeam(principal, teamName, name)).willCallRealMethod();
		ResponseEntity<?> response = subject.removeCloudProviderParametersFromTeam(principal, teamName, name);
		assertTrue(toRemove.getSharedWithTeams().size() == 0);
		assertTrue(response.getStatusCode().equals(HttpStatus.OK));
	}

	@Test
	public void testAddConfigurationToTeam() {
		getPrincipal();
		getAccount();
		getConfigurationResoure();
		getConfigurationsBelongingTeam();
		getTeamResoure(team);
		assertTrue(team.getConfigurationsBelongingToTeam().size() == 0);
		assertTrue(configuration.getSharedWithTeams().size() == 0);
		given(configuration.getAccount()).willReturn(account);
		given(teamService.findByNameAndAccountUsername(Mockito.anyString(), Mockito.anyString())).willReturn(team);
		given(teamService.save(team)).willReturn(team);
		given(cppService.findByNameAndAccountUsername(cppName, principalName)).willReturn(cpp);

		given(configDepParamsService.findByNameAndAccountUserName(configDepParamsName, principalName)).willReturn(configDepParams);

		given(configurationService.findByNameAndAccountUsername(Mockito.anyString(), Mockito.anyString())).willReturn(configuration);
		given(subject.addConfigurationToTeam(principal, configurationResource, teamName)).willCallRealMethod();
		ResponseEntity<?> configurationAdded = subject.addConfigurationToTeam(principal, configurationResource, teamName);
		assertTrue(configurationAdded.getStatusCode().equals(HttpStatus.OK));
		assertTrue(team.getConfigurationsBelongingToTeam().size() == 1);
		assertTrue(configuration.getSharedWithTeams().size() == 1);
	}

	@Test
	public void testRemoveConfigurationFromTeam() throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeySpecException, IOException, ApplicationDeployerException{

		getPrincipal();
		Set<Configuration> configurations = new HashSet<>();
		Configuration toRemove = mock(Configuration.class);
		String name = "toRemove";
		given(toRemove.getName()).willReturn(name);
		String configurationReference = "some_reference";
		given(toRemove.getReference()).willReturn(configurationReference);
		configurations.add(toRemove);
		given(toRemove.getAccount()).willReturn(account);
		given(team.getConfigurationsBelongingToTeam()).willReturn(configurations);
		team.setConfigurationsBelongingToTeam(team.getConfigurationsBelongingToTeam());
		Account account = mock(Account.class);
		given(account.getEmail()).willReturn("some email");
		Set<Account> accounts = new HashSet<>();
		accounts.add(account);
		team.accountsBelongingToTeam = accounts;
		given(team.getAccountsBelongingToTeam()).willReturn(accounts);
		Set<Team> teams = new HashSet<>();
		teams.add(team);
		given(toRemove.getSharedWithTeams()).willReturn(teams);
		assertTrue(toRemove.getSharedWithTeams().size() == 1);
		assertTrue(team.getConfigurationsBelongingToTeam().size() == 1);
		given(configurationService.findByNameAndAccountUsername(name, principalName)).willReturn(toRemove);
		given(toRemove.getAccount().getUsername()).willReturn(principalName);
		given(team.getAccount()).willReturn(account);
		given(team.getAccount().getUsername()).willReturn(principalName);
		given(teamService.findByNameAndAccountUsername(teamName, principalName)).willReturn(team);
		given(teamService.save(team)).willReturn(team);

		//set deployments
		List<DeploymentConfiguration> deploymentConfigurations = new ArrayList<>();
		Deployment deployment = mock(Deployment.class);
		String deploymentReference = "some_ref";
		given(deployment.getReference()).willReturn(deploymentReference);
		DeploymentConfiguration depConfiguration = mock(DeploymentConfiguration.class);
		given(depConfiguration.getConfigurationReference()).willReturn(configurationReference);
		given(deployment.getDeploymentConfiguration()).willReturn(depConfiguration);
		given(depConfiguration.getDeployment()).willReturn(deployment);
		deploymentConfigurations.add(depConfiguration);
		given(depConfigService.findAll()).willReturn(deploymentConfigurations);
		given(subject.removeConfigurationFromTeam(principal, teamName, name)).willCallRealMethod();
		ResponseEntity<?> response = subject.removeConfigurationFromTeam(principal, teamName, name);
		assertTrue(toRemove.getSharedWithTeams().size() == 0);
		assertTrue(response.getStatusCode().equals(HttpStatus.OK));
	}

	@Test
	public void testRemoveConfigDepParamsFromTeam() throws IOException{

		getPrincipal();
		Set<ConfigurationDeploymentParameters> configDepParams = new HashSet<>();
		ConfigurationDeploymentParameters toRemove = mock(ConfigurationDeploymentParameters.class);
		String name = "toRemove";
		given(toRemove.getName()).willReturn(name);
		configDepParams.add(toRemove);
		given(toRemove.getAccount()).willReturn(account);
		given(team.getConfigDepParamsBelongingToTeam()).willReturn(configDepParams);
		team.setConfigDepParamsBelongingToTeam(team.getConfigDepParamsBelongingToTeam());
		Account account = mock(Account.class);
		given(account.getEmail()).willReturn("some email");
		Set<Account> accounts = new HashSet<>();
		accounts.add(account);
		team.accountsBelongingToTeam = accounts;
		given(team.getAccountsBelongingToTeam()).willReturn(accounts);
		Set<Team> teams = new HashSet<>();
		teams.add(team);
		given(toRemove.getSharedWithTeams()).willReturn(teams);
		assertTrue(toRemove.getSharedWithTeams().size() == 1);
		assertTrue(team.getConfigDepParamsBelongingToTeam().size() == 1);
		given(configDepParamsService.findByNameAndAccountUserName(name, principalName)).willReturn(toRemove);
		given(toRemove.getAccount().getUsername()).willReturn(principalName);
		given(team.getAccount()).willReturn(account);
		given(team.getAccount().getUsername()).willReturn(principalName);
		given(teamService.findByNameAndAccountUsername(teamName, principalName)).willReturn(team);
		given(teamService.save(team)).willReturn(team);
		given(subject.removeConfigurationDeploymentParametersFromTeam(principal, teamName, name)).willCallRealMethod();
		ResponseEntity<?> response = subject.removeConfigurationDeploymentParametersFromTeam(principal, teamName, name);
		assertTrue(toRemove.getSharedWithTeams().size() == 0);
		assertTrue(response.getStatusCode().equals(HttpStatus.OK));
	}

	@Test
	public void testOwnerCanSeeOtherManagers() {
		teamOwnerSetup();
		TeamResource response = subject.getTeamByName(request, principal, teamName);
		assertTrue(response.getManagerEmails().size() == 1);
	}

	@Test
	public void testOwnerCanSeeAllMemberEmails() {
		teamOwnerSetup();
		addAccountsToTeam();
		TeamResource response = subject.getTeamByName(request, principal, teamName);
		assertTrue(response.getMemberAccountEmails().size() == 2);
	}

	@Test(expected = TeamNotFoundException.class)
	public void testNonOwnerAndNonManagerCannotSeeOtherManagers() {
		getRequest();
		getPrincipal();
		getAccount();
		String domainReference = "someDomainRef";
		Account someOtherAccount = mock(Account.class);
		given(team.getAccount()).willReturn(someOtherAccount);
		given(team.getName()).willReturn(teamName);
		given(subject.getTeamByName(request, principal, teamName)).willCallRealMethod();
		subject.getTeamByName(request, principal, teamName);
	}

	@Test
	public void testManagerButNonOwnerCanSeeOtherManagers() {
		managerNotTeamOwnerSetup();
		given(subject.getTeamByName(request, principal, teamName)).willCallRealMethod();
		TeamResource response = subject.getTeamByName(request, principal, teamName);
		assertTrue(response.getManagerEmails().size() == 1);
	}

	@Test
	public void testManagerButNonOwnerCanSeeMemberEmails() {
		managerNotTeamOwnerSetup();
		given(subject.getTeamByName(request, principal, teamName)).willCallRealMethod();
		TeamResource response = subject.getTeamByName(request, principal, teamName);
		assertTrue(response.getMemberAccountEmails().size() == 2);
	}

	@Test
	public void testAddTeamContactTeamOwnerPass() throws AccountNotFoundException {
		getAccount();
		getPrincipal();
		getRequest();
		getTeamResoure(team);
		Set<String> teamContactEmails = new HashSet<>();
		teamContactEmails.add("anyteamcontactemail");
		given(team.getAccount()).willReturn(account);
		teamResource.setTeamContactEmails(teamContactEmails);
		given(teamService.findByName(teamResource.getName())).willReturn(team);
		given(teamService.setManagerUsernamesAndEmails(isA(TeamResource.class), isA(String.class))).willCallRealMethod();
		given(teamService.populateTeamContactEmails(isA(Team.class), isA(TeamResource.class), isA(String.class))).willReturn(teamResource);
		given(teamService.findByDomainReference(team.getDomainReference())).willReturn(team);
		given(teamService.setContactEmails(isA(Set.class), isA(Team.class))).willCallRealMethod();
		given(teamService.save(team)).willReturn(team);
		String emails = "contact1@ebi,contact2@ebi";
		given(subject.addTeamContactEmails(request, principal, team.getName(), emails)).willCallRealMethod();
		ResponseEntity<?> response = subject.addTeamContactEmails(request, principal, team.getName(), emails);
		assertTrue(response.getStatusCode().equals(HttpStatus.OK));
	}

	@Test
	public void testRemoveTeamContactTeamOwnerPass() {
		getAccount();
		getPrincipal();
		getRequest();
		getTeamResoure(team);
		given(teamService.findByName(teamResource.getName())).willReturn(team);
		String emailToRemove = "someemail";
		given(subject.removeTeamContactEmail(request, principal, teamName, emailToRemove)).willCallRealMethod();
		subject.removeTeamContactEmail(request, principal, teamName, emailToRemove);
	}

	@Test(expected = TeamAccessDeniedException.class)
	public void testAddTeamContactNonTeamOwnerFail() {
		getAccount();
		getPrincipal();
		getRequest();
		getTeamResoure(team);
		Account notTeamOwner = mock(Account.class);
		given(notTeamOwner.getUsername()).willReturn("notowner");
		given(team.getAccount()).willReturn(notTeamOwner);
		given(teamService.findByName(teamResource.getName())).willReturn(team);
		given(teamService.findByDomainReference(team.getDomainReference())).willReturn(team);
		String emails = "contact1@ebi,contact2@ebi";
		given(subject.addTeamContactEmails(request, principal, team.getName(), emails)).willCallRealMethod();
		subject.addTeamContactEmails(request, principal, team.getName(), emails);
	}

	@Test(expected = TeamAccessDeniedException.class)
	public void testRemoveTeamContactTeamNotOwnerFail() throws AccountNotFoundException {
		getAccount();
		getPrincipal();
		getRequest();
		getTeamResoure(team);
		Account notTeamOwner = mock(Account.class);
		given(notTeamOwner.getUsername()).willReturn("notowner");
		given(team.getAccount()).willReturn(notTeamOwner);
		given(teamService.findByName(teamResource.getName())).willReturn(team);
		String emailToRemove = "someemail";
		given(subject.removeTeamContactEmail(request, principal, teamName, emailToRemove)).willCallRealMethod();
		subject.removeTeamContactEmail(request, principal, teamName, emailToRemove);
	}

	@Test
	public void testAddToDefaultTeam() {
		getPrincipal();
		getAccount();
		getRequest();
		Team team = new Team();
		team.setName("teamName");
		team.getAccountsBelongingToTeam().add(account);
		team.setDomainReference("domainReference");
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return team;
			}
		}).when(teamService).addAccountToDefaultTeamsByEmail(account);
		doCallRealMethod().when(subject).addAccountToDefaultTeamsByEmail(request, response, principal);
		subject.addAccountToDefaultTeamsByEmail(request, response, principal);
		assertTrue(team.accountsBelongingToTeam.size() == 1);
	}

	/*@Test
	public void testLeaveTeam() throws Exception{
		getPrincipal();
		this.getTeamResoure(team);
		given(principal.getName()).willReturn(principalName);
		given(teamService.leaveTeam(request, principal,
				deploymentService, configurationService, configDepParamsService,
				deploymentRestController, teamResource)).willReturn(true);
		given(subject.leaveTeam(request, principal, teamResource)).willCallRealMethod();
		ResponseEntity<?> response = subject.leaveTeam(request, principal, teamResource);
		assertTrue(response.getStatusCode().equals(HttpStatus.OK));
	}
	
	@Test
	public void testLeaveTeamTeamNotFound() throws Exception{
		getPrincipal();
		this.getTeamResoure(team);
		given(principal.getName()).willReturn(principalName);
		given(teamService.findByName(teamName)).willCallRealMethod();
		given(teamRepository.findByName(teamName)).willThrow(TeamNotFoundException.class);
		given(teamService.leaveTeam(request, principal,
				deploymentService, configurationService, configDepParamsService,
				deploymentRestController, teamResource)).willCallRealMethod();
		given(subject.leaveTeam(request, principal, teamResource)).willCallRealMethod();
		ResponseEntity<?> response = subject.leaveTeam(request, principal, teamResource);
		assertTrue(response.getStatusCode().equals(HttpStatus.NOT_MODIFIED));
	}*/

	private void managerNotTeamOwnerSetup() {
		getRequest();
		getAccount();
		addAccountsToTeam();
		String domainReference = "someDomainRef";
		Account someOtherAccount = mock(Account.class);
		given(team.getAccount()).willReturn(someOtherAccount);
		String someOtherAccountUsername = "someotherusername";
		given(someOtherAccount.getUsername()).willReturn(someOtherAccountUsername);
		given(team.getName()).willReturn(teamName);
		given(teamResource.getName()).willReturn(teamName);
		given(team.getDomainReference()).willReturn(domainReference);
		Set<User> managers = new HashSet<>();
		User managerOne = mock(User.class);
		given(managerOne.getEmail()).willReturn("emailOne");
		given(managerOne.getUserName()).willReturn("managerUser");
		managers.add(managerOne);
		Answer domainManagerAnswer = new Answer<Collection<User>>() {
			@Override
			public Collection<User> answer(InvocationOnMock invocation) {
				return managers;
			}
		};
		given(principal.getName()).willReturn(someOtherAccountUsername);
		given(teamService.findByNameAndAccountUsername(teamName, someOtherAccountUsername)).willReturn(team);
		given(teamService.findByName(teamName)).willReturn(team);
		given(teamService.setManagerUsernamesAndEmails(isA(TeamResource.class), isA(String.class))).willCallRealMethod();
		given(teamService.checkIfOwnerOrManagerOfTeam(teamName, principal, token)).willCallRealMethod();
		when(domainService.getAllManagersFromDomain(domainReference, token)).thenAnswer(domainManagerAnswer);
		given(teamService.populateTeamContactEmails(isA(Team.class), isA(TeamResource.class), isA(String.class))).willCallRealMethod();
		given(teamService.populateTeamMemberEmails(isA(Team.class), isA(TeamResource.class), isA(String.class))).willCallRealMethod();
	}

	private void teamOwnerSetup() {
		getRequest();
		getPrincipal();
		getAccount();
		String domainReference = "someDomainRef";
		given(team.getAccount()).willReturn(account);
		given(team.getName()).willReturn(teamName);
		given(teamResource.getName()).willReturn(teamName);
		given(team.getDomainReference()).willReturn(domainReference);
		Set<User> managers = new HashSet<>();
		User managerOne = mock(User.class);
		given(managerOne.getEmail()).willReturn("emailOne");
		managers.add(managerOne);
		Answer domainManagerAnswer = new Answer<Collection<User>>() {
			@Override
			public Collection<User> answer(InvocationOnMock invocation) {
				return managers;
			}
		};
		given(teamService.findByNameAndAccountUsername(teamName, principalName)).willReturn(team);
		given(teamService.findByName(teamName)).willReturn(team);
		given(teamService.setManagerUsernamesAndEmails(isA(TeamResource.class), isA(String.class))).willCallRealMethod();
		given(teamService.checkIfOwnerOrManagerOfTeam(teamName, principal, token)).willCallRealMethod();
		when(domainService.getAllManagersFromDomain(domainReference, token)).thenAnswer(domainManagerAnswer);
		given(teamService.populateTeamContactEmails(isA(Team.class), isA(TeamResource.class), isA(String.class))).willCallRealMethod();
		given(teamService.setManagerUsernamesAndEmails(isA(TeamResource.class), isA(String.class))).willCallRealMethod();
		given(teamService.populateTeamMemberEmails(isA(Team.class), isA(TeamResource.class), isA(String.class))).willCallRealMethod();
		given(subject.getTeamByName(request, principal, teamName)).willCallRealMethod();
	}

	private void getCppResoure(){
		given(cppResource.getName()).willReturn(cppName);
		Set<Team> cppSharedWith = new HashSet<>();
		given(cpp.getSharedWithTeams()).willReturn(cppSharedWith);
	}

	private void getApplicationResoure(){
		given(applicationResource.getName()).willReturn(applicationName);
		Set<Team> applicationSharedWith = new HashSet<>();
		given(application.getSharedWithTeams()).willReturn(applicationSharedWith);
	}

	private void getConfigurationResoure(){
		given(configurationResource.getName()).willReturn(configurationName);
		given(configurationResource.getCloudProviderParametersName()).willReturn(cppName);
		given(configurationResource.getDeploymentParametersName()).willReturn(configDepParamsName);
		Set<Team> configurationSharedWith = new HashSet<>();
		given(configuration.getSharedWithTeams()).willReturn(configurationSharedWith);
	}

	private void getConfigurationDeploymentParametersResource(){
		given(configDepParamsResource.getName()).willReturn(configDepParamsName);
		Set<Team> configDepParamsSharedWith = new HashSet<>();
		given(configDepParams.getSharedWithTeams()).willReturn(configDepParamsSharedWith);
	}

	private void addAccountsToTeam(){

		given(someAccount.getEmail()).willReturn(userEmail);
		given(someotherAccount.getEmail()).willReturn("anEmail");
		given(someAccount.getUsername()).willReturn("someusername");
		given(someotherAccount.getUsername()).willReturn(someotherusername);
		Set<Account> accounts = new HashSet<>();
		accounts.add(someAccount);accounts.add(someotherAccount);
		team.getAccountsBelongingToTeam().addAll(accounts);
		Mockito.when(team.getAccountsBelongingToTeam()).thenReturn(accounts);
	}

	private void getApplicationsBelongingTeam(){
		Set<Application> applications = new HashSet<>();
		given(team.getApplicationsBelongingToTeam()).willReturn(applications);
	}

	private void getCppBelongingTeam(){
		Set<CloudProviderParameters> cpps = new HashSet<>();
		given(team.getCppBelongingToTeam()).willReturn(cpps);
	}

	private void getConfigurationsBelongingTeam(){
		Set<Configuration> configurations = new HashSet<>();
		given(team.getConfigurationsBelongingToTeam()).willReturn(configurations);
	}

	private void getConfigDepParamsBelongingTeam(){
		Set<ConfigurationDeploymentParameters> configDepParams = new HashSet<>();
		given(team.getConfigDepParamsBelongingToTeam()).willReturn(configDepParams);
	}

	private void getMemberOfTeams(){
		Set<Team> teams = new HashSet<>();
		teams.add(team);teams.add(toRemove);
		given(someotherAccount.getMemberOfTeams()).willReturn(teams);
	}

	private void getTeamResoure(Team team){

		given(team.getAccount()).willReturn(account);
		given(team.getName()).willReturn(teamName);
		given(teamResource.getName()).willReturn(teamName);
		given(team.getDomainReference()).willReturn("a reference");
	}

	private void getTeamResoureNoDomain(Team team){

		given(team.getAccount()).willReturn(account);
		given(team.getName()).willReturn(teamName);
        given(teamResource.getMemberAccountEmails()).willReturn(Arrays.asList("anEmail"));
		given(teamResource.getName()).willReturn(teamName);
		given(team.getDomainReference()).willReturn(null);
	}

	private void getPrincipal(){
		given(principal.getName()).willReturn(principalName);
	}

	private void getAccount(){
		given(accountService.findByUsername(principalName)).willReturn(account);
		given(account.getUsername()).willReturn(principalName);
		String reference = "somereference";
		Date firstJoinedDate = new Date(2,2,2000);
		given(account.getFirstJoinedDate()).willReturn(firstJoinedDate);
		given(account.getReference()).willReturn(reference);
		given(account.getEmail()).willReturn("anEmail@test.com");
	}

	private void getRequest(){
		given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn(tokenArray);
	}

	private void getDomain(){
		String domainReference = "a domain reference";
		given(domain.getDomainReference()).willReturn(domainReference);
		given(domainService.createDomain("TEAM_"+team.getName().toUpperCase()+"_PORTAL", "Domain TEAM_"+team.getName()+"_PORTAL"+" created", token)).willReturn(domain);
		Mockito.when(domainService.getDomainByReference(domainReference, token)).thenReturn(domain);
		//domainService.createDomain("some name", "some description", token);
	}


	private void getFailDomainNull(){
		String domainReference = "a domain reference";
		given(domain.getDomainReference()).willReturn(null);
		given(domainService.createDomain("TEAM_"+team.getName().toUpperCase()+"_PORTAL", "Domain TEAM_"+team.getName()+"_PORTAL"+" created", token)).willReturn(null);
		given(domainService.getDomainByReference(domainReference, "token")).willReturn(null);

	}

	private void getFailDomainException(){
		String domainReference = "a domain reference";
		given(domain.getDomainReference()).willThrow(Exception.class);
		given(domainService.createDomain("TEAM_"+team.getName().toUpperCase()+"_PORTAL", "Domain TEAM_"+team.getName()+"_PORTAL"+" created", token)).willThrow(Exception.class);
		given(domainService.getDomainByReference(domainReference, "token")).willThrow(Exception.class);
		//domainService.createDomain("some name", "some description", token);
	}
}
