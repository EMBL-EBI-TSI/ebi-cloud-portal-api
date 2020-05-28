
package uk.ac.ebi.tsc.portal.api.application.controller;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.Is.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.repo.AccountRepository;
import uk.ac.ebi.tsc.portal.api.account.service.AccountService;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationRepository;
import uk.ac.ebi.tsc.portal.api.application.service.ApplicationNotFoundException;
import uk.ac.ebi.tsc.portal.api.application.service.ApplicationService;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentApplication;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentApplicationRepository;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentApplicationService;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;
import uk.ac.ebi.tsc.portal.api.team.repo.TeamRepository;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDownloader;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ProcessRunner;
import uk.ac.ebi.tsc.portal.clouddeployment.exceptions.ApplicationDownloaderException;
import uk.ac.ebi.tsc.portal.clouddeployment.model.ApplicationManifest;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class ApplicationRestControllerTest {
	ApplicationRepository mockApplicationRepo = mock(ApplicationRepository.class);
	ApplicationDownloader mockApplicationDownloader = mock(ApplicationDownloader.class);
	AccountService accountServiceMock = mock(AccountService.class);
	ApplicationManifest applicationManifestMock = mock(ApplicationManifest.class);
	ApplicationService applicationService = mock(ApplicationService.class);
	AccountService accountService = mock(AccountService.class);
	DomainService domainService = mock(DomainService.class);
	uk.ac.ebi.tsc.aap.client.security.TokenHandler tokenHandler = mock(uk.ac.ebi.tsc.aap.client.security.TokenHandler.class);
	HttpServletRequest request = mock(HttpServletRequest.class);
	DeploymentApplicationService deploymentApplicationService = mock(DeploymentApplicationService.class);
	Principal principalMock = mock(Principal.class);
	ApplicationDownloader applicationDownloader = mock(ApplicationDownloader.class);
	User user = mock(User.class);
	Account accountMock = mock(Account.class);
	
	@MockBean
	ApplicationRestController subject; 

	private static final String APPS_ROOT_FOLDER = "/a/path/has/no/name";
	private static final int CREATED_HTTP_STATUS = 201;
	private static final int OK_HTTP_STATUS = 200;
	String tokenArray = "some token";
	String token = "token" ;


	@Before
	public void setUp() {
		Properties props = new Properties();
		props.put("be.applications.root", APPS_ROOT_FOLDER);
		ReflectionTestUtils.setField(subject, "applicationService", applicationService);
		ReflectionTestUtils.setField(subject, "accountService", accountService);
		ReflectionTestUtils.setField(subject, "applicationDownloader", applicationDownloader);
		ReflectionTestUtils.setField(applicationDownloader, "accountService", accountService);
		ReflectionTestUtils.setField(applicationDownloader, "accountService", accountService);
		ReflectionTestUtils.setField(subject, "tokenHandler", tokenHandler);
		ReflectionTestUtils.setField(subject, "deploymentApplicationService", deploymentApplicationService);
		ReflectionTestUtils.setField(subject, "applicationsRoot", APPS_ROOT_FOLDER);
		subject.setProperties(props);
		when(this.principalMock.getName()).thenReturn("A user name");
		when(this.accountMock.getId()).thenReturn(1L);
		when(this.accountMock.getUsername()).thenReturn("A user name");
		when(this.accountMock.getEmail()).thenReturn("an@email.com");
		when(this.accountMock.getPassword()).thenReturn("A password");
		when(this.accountMock.getOrganisation()).thenReturn("An organisation");
		when(this.accountMock.getMemberOfTeams()).thenReturn((Set<Team>)mock(Set.class));

	}

	/**
	 * Test if user can list applications associated with his/her account 
	 * @throws IOException
	 * @throws ApplicationDownloaderException
	 */
	@Test 
	public void
	can_list_all_application_for_account() throws IOException, ApplicationDownloaderException{

		when(principalMock.getName()).thenReturn("username");
		when(accountService.findByUsername("username")).thenReturn(accountMock);
		when(accountMock.getApplications()).thenReturn((Set<Application>) mock(Set.class));
		when(subject.getAllApplications(principalMock, new Sort("name.asc"))).thenCallRealMethod();
		Resources<ApplicationResource> resources = subject.getAllApplications(principalMock, new Sort("name.asc"));
		assertNotNull(resources);
	}

	/**
	 * Test if user can retrieve an application by his/her account user name and application name
	 * @throws IOException
	 * @throws ApplicationDownloaderException
	 */
	@Test public void
	can_get_application_by_account_name() throws IOException, ApplicationDownloaderException{

		String repoUri = "repoUri";
		String appName = "someName";

		Application application = mockApplication(repoUri, APPS_ROOT_FOLDER + File.separator + appName, appName);
		mockSavedApplication(application, repoUri);
		when(applicationService.findByAccountUsernameAndName(this.accountMock.getUsername(), appName)).thenReturn(application);
		when(subject.getApplicationByAccountUsernameAndName(principalMock, appName)).thenCallRealMethod();
		ApplicationResource applicationResource = subject.getApplicationByAccountUsernameAndName(principalMock, appName);
		assertNotNull(applicationResource);
	}

	/**
	 * Test if user can add an application to his/her own account using the repository URI
	 * @throws IOException
	 * @throws ApplicationDownloaderException
	 */
	@Test 
	public void
	can_add_application_given_repo_uri() throws IOException, ApplicationDownloaderException {

		String theUri = "blah";
		String theName = "an-app-has-no-name";
		Application mockApplication = mockApplication(theUri, APPS_ROOT_FOLDER + File.separator + theName, theName);
		ApplicationResource inputResource = new ApplicationResource(mockApplication);
		when(applicationService.findByAccountUsernameAndName(this.accountMock.getUsername(), inputResource.getName())).thenThrow(ApplicationNotFoundException.class);
		when(applicationService.save(mockApplication)).thenReturn(mockApplication);
		when(applicationDownloader.downloadApplication(APPS_ROOT_FOLDER, inputResource.getRepoUri(), this.accountMock.getUsername())).thenReturn(mockApplication);
		when(subject.add(principalMock, inputResource)).thenCallRealMethod();
		// do the request
		ResponseEntity response = subject.add(principalMock, inputResource);

		// check assertions
		assertThat(response.getStatusCode().value(), is(CREATED_HTTP_STATUS));

	}

	/**
	 * Test if user can delete an application using application name, from his/her own account 
	 * @throws IOException
	 * @throws ApplicationDownloaderException
	 */
	@Test public void
	can_delete_application_given_repo_uri() throws IOException, ApplicationDownloaderException {

		String theUri = "blah";
		String theName = "an-app-has-no-name";

		Application application = mockApplication(theUri, APPS_ROOT_FOLDER + File.separator + theName, theName);
		mockSavedApplication(application, theUri);
		when(subject.deleteApplicationByAccountUsernameAndName(principalMock, theName)).thenCallRealMethod();
		ResponseEntity response = subject.deleteApplicationByAccountUsernameAndName(principalMock, theName);

		assertThat(response.getStatusCode().value(), is(OK_HTTP_STATUS));
	}

	/**
	 * Test if user can list all shared applications associated with his/her own account
	 * @throws IOException
	 * @throws ApplicationDownloaderException
	 */
	@Test public void
	can_list_all_shared_application_for_account() throws IOException, ApplicationDownloaderException{

		String username = "username";

		when(accountService.findByUsername(username)).thenReturn(accountMock);

		//user is a member of one team which has one application
		Set<Team> teams = new HashSet<>();
		Team team = mock(Team.class);
		teams.add(team);
		Set<Application> applications = new HashSet<>();
		Application applicationMock = mock(Application.class);
		applications.add(applicationMock);
		when(team.getApplicationsBelongingToTeam()).thenReturn(applications);
		Answer teamAnswer = new Answer<Set<Team>>(){
			@Override
			public Set<Team> answer(InvocationOnMock invocation){
				return teams;
			}
		};
		when(accountMock.getMemberOfTeams()).thenAnswer(teamAnswer) ; 
		when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("some token");
		when(principalMock.getName()).thenReturn(username);
		getApplicationResource(applicationMock);
		Mockito.when(applicationService.getSharedApplicationsByAccount(
				any(Account.class),
				any(String.class),
				any(User.class))).thenReturn(applications);
		when(subject.getSharedApplicationByAccount(request, principalMock)).thenCallRealMethod();
		Resources<ApplicationResource> applicationList = subject.getSharedApplicationByAccount(request, principalMock);
		assertNotNull(applicationList);
		assertEquals(1, applicationList.getContent().size());

	}

	/**
	 * test that if user is not a member of any team
	 * @throws IOException
	 * @throws ApplicationDownloaderException
	 */
	@Test public void
	can_get_not_null_when_account_not_member_of_any_team() throws IOException, ApplicationDownloaderException{

		String username = "username";
		when(accountService.findByUsername(username)).thenReturn(accountMock);
		Set<Application> applications = new HashSet<>();
		Application applicationMock = mock(Application.class);
		applications.add(applicationMock);
		when(accountMock.getMemberOfTeams()).thenReturn(new HashSet<>()) ; 
		when(principalMock.getName()).thenReturn(username);
		getApplicationResource(applicationMock);
		when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("some token");
		when(subject.getSharedApplicationByAccount(request, principalMock)).thenCallRealMethod();
		Resources<ApplicationResource> applicationList = subject.getSharedApplicationByAccount(request, principalMock);
		assertNotNull(applicationList);
		assertEquals(0, applicationList.getContent().size());

	}

	/**
	 * test if an application is created from the manifest file
	 * @throws IOException
	 * @throws ApplicationDownloaderException
	 */
	@Test
	public void can_application_be_created_from_manifest() throws IOException, ApplicationDownloaderException{

		String applicationName = "App";
		String  deploymentParameterMock = "deploymentParameter";
		List<String> deploymentParameterCollection = new LinkedList<>();
		deploymentParameterCollection.add(deploymentParameterMock);
		applicationManifestMock.deploymentParameters = deploymentParameterCollection;
		applicationManifestMock.applicationName = applicationName;
		Application mockApplication = new ApplicationDownloader(accountServiceMock, new ProcessRunner()).fromManifestToApplication("repoUri", "path", accountMock, applicationManifestMock);
		assertNotNull(mockApplication);
		assertThat(mockApplication.getName(), is(applicationName));

	}

	/**
	 * test if user can see the applications he owns
	 */
	@Test
	public void can_user_see_owned_applications(){
		
		String username = "username";
		Sort sortMock = mock(Sort.class);
		when(principalMock.getName()).thenReturn(username);
		when(accountMock.getUsername()).thenReturn(username);
		when(accountService.findByUsername(username)).thenReturn(accountMock);
		Application accountMockApp = mock(Application.class);
		Set<Application> accountApplications = new HashSet<>();
		accountApplications.add(accountMockApp);
		when(applicationService.findByAccountUsername(username, sortMock)).thenReturn(accountApplications);
		getApplicationResource(accountMockApp);
		when(subject.getAllApplications(principalMock, sortMock)).thenCallRealMethod();
		Resources<ApplicationResource> applicationList = subject.getAllApplications(principalMock, sortMock);
		assertNotNull(applicationList);
		assertEquals(1, applicationList.getContent().size());
	}

	/**
	 * test if user can see the applications he owns
	 */
	@Test
	public void can_get_not_null_when_account_has_no_owned_applications(){

		ReflectionTestUtils.setField(subject, "applicationService", applicationService);
		ReflectionTestUtils.setField(subject, "accountService", accountService);
		String username = "username";
		when(principalMock.getName()).thenReturn(username);
		when(accountMock.getUsername()).thenReturn(username);
		when(accountService.findByUsername(username)).thenReturn(accountMock);
		when(applicationService.findByAccountUsername(username, new Sort("sort.name"))).thenReturn(new HashSet<>());
		when(subject.getAllApplications(principalMock, new Sort("name.asc"))).thenCallRealMethod();
		Resources<ApplicationResource> applicationList = subject.getAllApplications(principalMock, new Sort("name.asc"));
		assertNotNull(applicationList);
		assertEquals(0, applicationList.getContent().size());
	}

	@Test
	public void can_get_shared_applications_by_account(){

		this.getRequest();
		Application application = mock(Application.class);
		getApplicationResource(application);
		Set<Application> applications = new HashSet<>();
		applications.add(application);
		when(applicationService.getSharedApplicationsByAccount(any(Account.class), Mockito.anyString(), any(User.class))).thenReturn(applications);
		when(subject.getSharedApplicationByAccount(request, principalMock)).thenCallRealMethod();
		Resources resources = subject.getSharedApplicationByAccount(request, principalMock);
		assertEquals(resources.getContent().size(), 1);
	}

	@Test
	public void can_get_shared_applications_by_applicationName_pass() throws IOException, ApplicationDownloaderException{

		this.getRequest();
		String appName = "name";
		Application application = mockApplication("repoUri", "repoPath", appName);
		when(applicationService.findByAccountUsernameAndName(Mockito.anyString(), Mockito.anyString())).thenReturn(application);
		when(applicationService.getSharedApplicationByApplicationName(any(Account.class), Mockito.anyString(), any(User.class), Mockito.anyString())).thenReturn(application);
		when(subject.getSharedByName(request, principalMock, "application")).thenCallRealMethod();
		ApplicationResource resources = subject.getSharedByName(request, principalMock, "application");
		assertTrue(resources.getId() != null);
	}

	@Test(expected = ApplicationNotFoundException.class)
	public void can_get_shared_applications_by_applicationName_fail() throws IOException, ApplicationDownloaderException{
		
		this.getRequest();
		when(applicationService.findByAccountUsernameAndName(Mockito.anyString(), Mockito.anyString())).thenReturn(null);
		when(applicationService.getSharedApplicationByApplicationName(any(Account.class), Mockito.anyString(), any(User.class), Mockito.anyString())).thenReturn(null);
		when(subject.getSharedByName(request, principalMock, "application")).thenCallRealMethod();
		ApplicationResource resources = subject.getSharedByName(request, principalMock, "application");
	}
	
	@Test
	public void can_delete_application_repo_when_no_deployments() throws IOException, ApplicationDownloaderException{
		
		String repoPath = "APPS_ROOT_FOLDER + File.separator + theName";
		File file = new File(repoPath);
		file.createNewFile();
		assertTrue(file.exists()==true);
		
		String principalName = "principal";
		when(principalMock.getName()).thenReturn(principalName);
		
		String theUri = "blah";
		String theName = "an-app-has-no-name";

		Application application = mockApplication(theUri, repoPath, theName);
		mockSavedApplication(application, theUri);
		
		List<DeploymentApplication> depList = new ArrayList<>();
	
		Answer depListAnswer = new Answer<List<DeploymentApplication>>(){
			@Override
			public List<DeploymentApplication> answer(InvocationOnMock invocation){
				return depList;
			}
		};
		
		when(deploymentApplicationService.findByAccountIdAndRepoPath(application.getAccount().getId(), application.getRepoPath()))
		.thenAnswer(depListAnswer);
		
		when(applicationDownloader.removeApplication(repoPath, theUri)).thenCallRealMethod();
		when(subject.deleteApplicationByAccountUsernameAndName(principalMock, theName)).thenCallRealMethod();
		ResponseEntity appDeleted = subject.deleteApplicationByAccountUsernameAndName(principalMock, theName);
		
		assertTrue(file.exists()==false);
		assertTrue(appDeleted.getStatusCode() == HttpStatus.OK);
		file.delete();
	}
	
	@Test
	public void should_not_delete_application_repo_when_deployments_found() throws IOException, ApplicationDownloaderException{
		
		String repoPath = "APPS_ROOT_FOLDER + File.separator + theName";
		File file = new File(repoPath);
		file.createNewFile();
		assertTrue(file.exists()==true);
		
		String principalName = "principal";
		when(principalMock.getName()).thenReturn(principalName);
		
		String theUri = "blah";
		String theName = "an-app-has-no-name";

		Application application = mockApplication(theUri, repoPath, theName);
		mockSavedApplication(application, theUri);
		
		List<DeploymentApplication> depList = new ArrayList<>();
		DeploymentApplication depApp = mock(DeploymentApplication.class);
		depList.add(depApp);
		
		Answer depListAnswer = new Answer<List<DeploymentApplication>>(){
			@Override
			public List<DeploymentApplication> answer(InvocationOnMock invocation){
				return depList;
			}
		};
		
		when(deploymentApplicationService.findByAccountIdAndRepoPath(application.getAccount().getId(), application.getRepoPath()))
		.thenAnswer(depListAnswer);
		
		when(applicationDownloader.removeApplication(repoPath, theUri)).thenCallRealMethod();
		when(subject.deleteApplicationByAccountUsernameAndName(principalMock, theName)).thenCallRealMethod();
		ResponseEntity appDeleted = subject.deleteApplicationByAccountUsernameAndName(principalMock, theName);
		
		
		assertTrue(file.exists()==true);
		assertTrue(appDeleted.getStatusCode() == HttpStatus.OK);
		file.delete();
	}

	private void getApplicationResource(Application application){

		when(application.getAccount()).thenReturn(accountMock);
		when(application.getRepoUri()).thenReturn("repoUri");
	}

	private Application mockApplication(String repoUri, String repoPath, String name) throws IOException, ApplicationDownloaderException {

		Application mockApplication = mock(Application.class);
		when(mockApplication.getRepoPath()).thenReturn(repoPath);
		when(mockApplication.getName()).thenReturn(name);
		when(mockApplication.getRepoUri()).thenReturn(repoUri);
		when(mockApplication.getId()).thenReturn(1L);
		when(mockApplication.getAccount()).thenReturn(this.accountMock);
		when(accountMock.getId()).thenReturn(1L);

		return mockApplication;
	}

	private void mockSavedApplication(Application mockApplication, String repoUri) throws IOException, ApplicationDownloaderException {

		when(applicationService.findByAccountUsernameAndName(this.principalMock.getName(), mockApplication.getName())).thenReturn(mockApplication);
		when(applicationService.findById(1L)).thenReturn(mockApplication);
		when(applicationDownloader.removeApplication(mockApplication.getRepoPath(), repoUri )).thenReturn(0);
		when(applicationDownloader.downloadApplication(APPS_ROOT_FOLDER, null, repoUri)).thenReturn(mockApplication);

	}

	private SharedApplicationResource mockSharedApplicationResource(String userEmail, String appName){

		SharedApplicationResource sharedApplicationResourceMock = mock(SharedApplicationResource.class);

		when(sharedApplicationResourceMock.getApplicationName()).thenReturn(appName);
		when(sharedApplicationResourceMock.getUserEmail()).thenReturn(userEmail);
		
		List<Account> accounts = new ArrayList<>();
		accounts.add(accountMock);
		given(accountService.findByEmail(userEmail)).willReturn(accounts.get(0));
		when(accountMock.getApplications()).thenReturn((Set<Application>) mock(Set.class));
		when(accountService.save(accountMock)).thenReturn(accountMock);

		return sharedApplicationResourceMock;

	}

	private void mockSavedApplicationNotToBeFound(Application mockApplication, String repoUri) throws IOException, ApplicationDownloaderException {
		when(applicationService.findByAccountUsernameAndName(this.principalMock.getName(), mockApplication.getName())).thenThrow(new ApplicationNotFoundException("noapp"));
	}

	private void getRequest(){
		given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn(tokenArray);
	}

}
