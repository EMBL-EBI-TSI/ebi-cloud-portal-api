package uk.ac.ebi.tsc.portal.api.deployment.controller;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.spec.InvalidKeySpecException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.service.AccountService;
import uk.ac.ebi.tsc.portal.api.application.controller.InvalidApplicationInputException;
import uk.ac.ebi.tsc.portal.api.application.controller.InvalidApplicationInputValueException;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationCloudProvider;
import uk.ac.ebi.tsc.portal.api.application.service.ApplicationNotFoundException;
import uk.ac.ebi.tsc.portal.api.application.service.ApplicationNotFoundUnderTeamException;
import uk.ac.ebi.tsc.portal.api.application.service.ApplicationService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.controller.CloudProviderParametersCopyResource;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopy;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParametersNotFoundException;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParametersNotSharedException;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParametersService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyService;
import uk.ac.ebi.tsc.portal.api.configuration.controller.InvalidConfigurationInputException;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigDeploymentParamsCopy;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;
import uk.ac.ebi.tsc.portal.api.configuration.service.*;
import uk.ac.ebi.tsc.portal.api.deployment.repo.Deployment;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentApplication;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentApplicationCloudProvider;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentConfiguration;
import uk.ac.ebi.tsc.portal.api.deployment.service.CloudCredentialNotUsableForApplicationException;
import uk.ac.ebi.tsc.portal.api.deployment.service.ConfigurationNotUsableForApplicationException;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentApplicationService;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentConfigurationService;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentGeneratedOutputService;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentNotFoundException;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentSecretService;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.encryptdecrypt.security.EncryptionService;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;
import uk.ac.ebi.tsc.portal.api.team.service.TeamNameInvalidInputException;
import uk.ac.ebi.tsc.portal.api.team.service.TeamService;
import uk.ac.ebi.tsc.portal.api.volumeinstance.service.VolumeInstanceService;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDeployer;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDeployerHelper;
import uk.ac.ebi.tsc.portal.clouddeployment.exceptions.ApplicationDeployerException;
import uk.ac.ebi.tsc.portal.usage.deployment.service.DeploymentIndexService;
import uk.ac.ebi.tsc.portal.usage.tracker.DeploymentStatusTracker;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
public class DeploymentRestControllerTest
 {

	private static final String A_CLOUD_PROVIDER_PARAMS_NAME = "OS TEST";
	public final String A_USER_NAME = "A User Name";
	public final String A_CLOUD_PROVIDER = "OSTACK";

	@MockBean
	private DeploymentService deploymentService;

	@MockBean
	private ConfigurationService configurationService;


	@MockBean
	private ConfigurationDeploymentParametersService configurationDeploymentParametersService;

	@MockBean
	private TeamService teamService;

	@MockBean
	DomainService domainService;

	@MockBean
	DeploymentConfigurationService deploymentConfigurationService;

	@MockBean
	DeploymentApplicationService deploymentApplicationService;

	@MockBean
	DeploymentRestController subject;

	@MockBean
	Principal principal;

	String tempKey = "dGhlcG9ydGFsZGV2ZWxvcGVkYnl0c2lpc2F3ZXNvbWU=";

	@MockBean
	CloudProviderParamsCopyService cloudProviderParametersCopyService;

	@MockBean
	CloudProviderParametersService cloudProviderParametersService;

	@MockBean
	ConfigDeploymentParamsCopyService configurationDeploymentParamsCopyService;


	@MockBean
	DeploymentConfiguration deploymentConfiguration;

	@MockBean
	CloudProviderParameters cloudProviderParameters;

	@MockBean
	AccountService accountService;

	@MockBean
	ApplicationService applicationService;

	@MockBean
	VolumeInstanceService volumeInstanceService;

	@MockBean
	ApplicationDeployer applicationDeployer;

	@MockBean
	DeploymentStatusTracker deploymentStatusTracker;

	@MockBean
	DeploymentIndexService deploymentIndexService;

	@MockBean
	EncryptionService encryptionService;

	@MockBean
	DeploymentSecretService deploymentSecretService;

	@MockBean
	DeploymentGeneratedOutputService deploymentGeneratedOutputService;

	@MockBean
	ApplicationDeployerHelper applicationDeployerHelper;

	String cppReference = "cppReference";
	@Before 
	public void setUp() {
		ReflectionTestUtils.setField(subject, "deploymentService", deploymentService);
		ReflectionTestUtils.setField(subject, "cloudProviderParametersCopyService", cloudProviderParametersCopyService);
		ReflectionTestUtils.setField(subject, "accountService", accountService);
		ReflectionTestUtils.setField(subject, "applicationService", applicationService);
		ReflectionTestUtils.setField(subject, "deploymentApplicationService", deploymentApplicationService);
		ReflectionTestUtils.setField(subject, "configurationService", configurationService);
		ReflectionTestUtils.setField(subject, "teamService", teamService);
		ReflectionTestUtils.setField(subject, "cloudProviderParametersService", cloudProviderParametersService);
		ReflectionTestUtils.setField(subject, "configDeploymentParamsCopyService", configurationDeploymentParamsCopyService);
		ReflectionTestUtils.setField(configurationService, "cppService", cloudProviderParametersService);
		ReflectionTestUtils.setField(subject, "applicationDeployer", applicationDeployer);
		ReflectionTestUtils.setField(subject, "applicationDeployerHelper", applicationDeployerHelper);
		Properties props = new Properties();
		props.put("be.applications.root", "blah");    
		props.put("be.deployments.root", "bleh");
		props.put("os.user.name", "blih");
		props.put("os.password", "bloh");
		props.put("os.tenancy.name", "bluh");
		props.put("os.auth.url", "blyh");
		subject.setProperties(props);
	}

	@Test
	public void can_delete_deployment_given_id()  {

		String theId = "blah";
		deployment(theId);
		CloudProviderParameters mockCloudProviderParameters = mock(CloudProviderParameters.class);
		when(mockCloudProviderParameters.getReference()).thenReturn(cppReference);
		when(cloudProviderParametersService.findByNameAndAccountUsername(A_CLOUD_PROVIDER_PARAMS_NAME, A_USER_NAME))
		.thenReturn(mockCloudProviderParameters);
		when(cloudProviderParametersService.findByReference(cppReference)).thenReturn(mockCloudProviderParameters);
		Account mockAccount = mock(Account.class);
		when(mockAccount.getUsername()).thenReturn(A_USER_NAME);
		when(mockAccount.getId()).thenReturn(1L);
		when(mockAccount.getEmail()).thenReturn("an@email.com");
		when(mockAccount.getPassword()).thenReturn("A password");
		when(mockAccount.getOrganisation()).thenReturn("An organisation");
		when(mockCloudProviderParameters.getAccount()).thenReturn(mockAccount);
		when( subject.removeDeploymentByReference(principal, theId)).thenCallRealMethod();
		ResponseEntity response = subject.removeDeploymentByReference(principal, theId);

		assertThat(response.getStatusCode().value(), is(200));
	}

	@Test
	public void can_receive_notification_that_deployment_is_ready_for_teardown()
			throws IOException, ApplicationDeployerException, NoSuchPaddingException, InvalidKeyException,
			NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException,
			InvalidAlgorithmParameterException, InvalidKeySpecException {
		String theId = "foo";
		deployment(theId);

		CloudProviderParameters mockCloudProviderParameters = mock(CloudProviderParameters.class);
		when(cloudProviderParametersService.findByNameAndAccountUsername(A_CLOUD_PROVIDER_PARAMS_NAME, A_USER_NAME))
		.thenReturn(mockCloudProviderParameters);
		when(subject.readyToTearDown(tempKey, theId)).thenCallRealMethod();
		ResponseEntity response = subject.readyToTearDown(tempKey, theId);
		assertThat(response.getStatusCode().is2xxSuccessful(), is(true));
	}

	@Test
	public void ignores_teardown_notification_unless_the_correct_secret_is_given()
			throws IOException, ApplicationDeployerException, NoSuchPaddingException, InvalidKeyException,
			NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException,
			InvalidAlgorithmParameterException, InvalidKeySpecException {
		String anIncorrectKey = "hey";
		when(subject.readyToTearDown(anIncorrectKey, null)).thenCallRealMethod();
		ResponseEntity response = subject.readyToTearDown(anIncorrectKey, null);
		assertThat(response.getStatusCode().value(), is(404));
	}

	@Test(expected = DeploymentNotFoundException.class)
	public void returns_appropriate_error_when_deployment_not_found() throws IOException, ApplicationDeployerException,
	NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException,
	BadPaddingException, InvalidAlgorithmParameterException, InvalidKeySpecException {
		String aNonExistentDeployment = "foo";
		when(deploymentService.findByReference(aNonExistentDeployment)).thenThrow(DeploymentNotFoundException.class);
		when(subject.readyToTearDown(tempKey, aNonExistentDeployment)).thenCallRealMethod();
		subject.readyToTearDown(tempKey, aNonExistentDeployment);
	}

	@Test
	public void triggers_deletion_of_deployment_when_receiving_teardown_notification()
			throws IOException, ApplicationDeployerException, NoSuchPaddingException, InvalidKeyException,
			NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException,
			InvalidAlgorithmParameterException, InvalidKeySpecException {
		String aDeploymentReference = "bar";
		Deployment mockDeployment = deployment(aDeploymentReference);
		when(mockDeployment.getId()).thenReturn(1234L);
		CloudProviderParameters mockCloudProviderParameters = mock(CloudProviderParameters.class);
		when(cloudProviderParametersService.findByNameAndAccountUsername(A_CLOUD_PROVIDER_PARAMS_NAME, A_USER_NAME))
		.thenReturn(mockCloudProviderParameters);
		when(subject.readyToTearDown(tempKey, aDeploymentReference)).thenCallRealMethod();
		subject.readyToTearDown(tempKey, aDeploymentReference);

		verify(deploymentService).delete(1234L);
	}

	@Test
	public void can_recognise_an_IP_has_been_given() throws IOException, ApplicationDeployerException,
	NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException,
	BadPaddingException, InvalidAlgorithmParameterException, InvalidKeySpecException {
		String anIp = "123.123.123.123";
		String aReference = "bar";
		Deployment mockDeployment = deployment(aReference);
		when(mockDeployment.getAccessIp()).thenReturn(anIp);
		when(deploymentService.findByAccessIp(anIp)).thenReturn(mockDeployment);

		CloudProviderParameters mockCloudProviderParameters = mock(CloudProviderParameters.class);
		when(cloudProviderParametersService.findByNameAndAccountUsername(A_CLOUD_PROVIDER_PARAMS_NAME, A_USER_NAME))
		.thenReturn(mockCloudProviderParameters);
		when(subject.readyToTearDown(tempKey, anIp)).thenCallRealMethod();
		subject.readyToTearDown(tempKey, anIp);

		verify(deploymentService).findByAccessIp(anIp);
	}

	@Test(expected = NullPointerException.class)
	public void test_if_deployment_throws_exception_null_cloud_providers()
			throws InvalidApplicationInputValueException, ConfigurationNotUsableForApplicationException, CloudCredentialNotUsableForApplicationException, IOException {

		DeploymentResource deploymentResourceMock = mock(DeploymentResource.class);
		String accountUserName = "a_name";
		String appName = "an_app";

		when(principal.getName()).thenReturn(accountUserName);
		Account mockAccount = mock(Account.class);
		when(mockAccount.getUsername()).thenReturn(accountUserName);
		when(accountService.findByUsername(accountUserName)).thenReturn(mockAccount);

		Application applicationMock = mock(Application.class);
		when(applicationMock.getName()).thenReturn(appName);
		when(applicationMock.getAccount()).thenReturn(mockAccount);
		Set applicationCollection = new HashSet<>();
		applicationCollection.add(applicationMock);

		when(applicationService.findByAccountUsername(accountUserName,new Sort("sort.name"))).thenReturn(applicationCollection);
		when(deploymentResourceMock.getApplicationAccountUsername()).thenReturn(accountUserName);
		when(deploymentResourceMock.getApplicationName()).thenReturn(appName);
		when(deploymentResourceMock.getConfigurationAccountUsername()).thenReturn(accountUserName);
		when(deploymentResourceMock.getConfigurationName()).thenReturn("some_configuration_name");
		when(applicationService.findByAccountUsernameAndName(accountUserName, appName))
		.thenReturn(applicationMock);
		HttpServletRequest request = new MockHttpServletRequest();
		when(subject.addDeployment(request, principal, deploymentResourceMock)).thenCallRealMethod();
		subject.addDeployment(request, principal, deploymentResourceMock);

	}

	 @Test(expected = TeamNameInvalidInputException.class)
	 public void test_if_team_deployment_throws_exception_empty_team_name() throws CloudCredentialNotUsableForApplicationException, IOException, ConfigurationNotUsableForApplicationException {
		 DeploymentResource deploymentResourceMock = mock(DeploymentResource.class);
		 HttpServletRequest request = new MockHttpServletRequest();
		 when(subject.deployForTeamShared(request, principal, "", deploymentResourceMock)).thenCallRealMethod();
		 subject.deployForTeamShared(request, principal, "", deploymentResourceMock);
	 }

	 @Test(expected = ApplicationNotFoundUnderTeamException.class)
	 public void test_if_team_deployment_throws_exception_application_not_shared() throws CloudCredentialNotUsableForApplicationException, IOException, ConfigurationNotUsableForApplicationException {
		 String applicationName = "bla_app";
		 String configuration = "bla_config";
		 String teamName = "team_football";

		 DeploymentResource deploymentResourceMock = mock(DeploymentResource.class);
		 Application application = mock(Application.class);
		 Set<Application> applicationSet = new HashSet<>();
		 applicationSet.add(application);

		 Team team = mock(Team.class);
		 given(application.getName()).willReturn("some_app");
		 given(team.getApplicationsBelongingToTeam()).willReturn(applicationSet);
		 given(deploymentResourceMock.getApplicationName()).willReturn(applicationName);
		 given(deploymentResourceMock.getConfigurationName()).willReturn(configuration);

		 HttpServletRequest request = new MockHttpServletRequest();
		 when(teamService.findByName(teamName)).thenReturn(team);
		 when(teamService.findSharedApplicationWitinTeam(team, applicationName)).thenCallRealMethod();
		 when(subject.deployForTeamShared(request, principal, teamName, deploymentResourceMock)).thenCallRealMethod();
		 subject.deployForTeamShared(request, principal, teamName, deploymentResourceMock);
	 }

	 @Test(expected = ConfigurationNotFoundUnderTeamException.class)
	 public void test_if_team_deployment_throws_exception_configuration_not_shared() throws CloudCredentialNotUsableForApplicationException, IOException, ConfigurationNotUsableForApplicationException {
		 String applicationName = "bla_app";
		 String configurationName = "bla_config";
		 String accountUserName = "bla_username";
		 String teamName = "team_football";

		 DeploymentResource deploymentResourceMock = mock(DeploymentResource.class);
		 Application application = mock(Application.class);
		 Account account = mock(Account.class);
		 Set<Application> applicationSet = new HashSet<>();
		 applicationSet.add(application);
		 Configuration configuration = mock(Configuration.class);
		 Set<Configuration> configurationSet = new HashSet<>();
		 configurationSet.add(configuration);

		 Team team = mock(Team.class);
		 given(application.getName()).willReturn(applicationName);
		 given(configuration.getName()).willReturn("some_config");
		 given(account.getUsername()).willReturn(accountUserName);
		 given(application.getAccount()).willReturn(account);
		 given(team.getApplicationsBelongingToTeam()).willReturn(applicationSet);
		 given(team.getConfigurationsBelongingToTeam()).willReturn(configurationSet);
		 given(deploymentResourceMock.getApplicationName()).willReturn(applicationName);
		 given(deploymentResourceMock.getConfigurationName()).willReturn(configurationName);

		 HttpServletRequest request = new MockHttpServletRequest();
		 when(teamService.findByName(teamName)).thenReturn(team);
		 when(teamService.findSharedApplicationWitinTeam(team, applicationName)).thenCallRealMethod();
		 when(teamService.findSharedConfigurationWitinTeam(team, configurationName)).thenCallRealMethod();
		 when(subject.deployForTeamShared(request, principal, teamName, deploymentResourceMock)).thenCallRealMethod();
		 subject.deployForTeamShared(request, principal, teamName, deploymentResourceMock);
	 }

	 @Test
	 public void test_if_team_deployment_succeeds() throws CloudCredentialNotUsableForApplicationException, IOException, ConfigurationNotUsableForApplicationException {
		 String applicationName = "bla_app";
		 String configurationName = "bla_config";
		 String accountUserName = "bla_username";
		 String teamName = "team_football";

		 DeploymentResource deploymentResourceMock = mock(DeploymentResource.class);
		 Application application = mock(Application.class);
		 Account account = mock(Account.class);
		 Set<Application> applicationSet = new HashSet<>();
		 applicationSet.add(application);
		 Configuration configuration = mock(Configuration.class);
		 Set<Configuration> configurationSet = new HashSet<>();
		 configurationSet.add(configuration);

		 Team team = mock(Team.class);
		 given(application.getName()).willReturn(applicationName);
		 given(configuration.getName()).willReturn(configurationName);
		 given(account.getUsername()).willReturn(accountUserName);
		 given(application.getAccount()).willReturn(account);
		 given(configuration.getAccount()).willReturn(account);
		 given(team.getApplicationsBelongingToTeam()).willReturn(applicationSet);
		 given(team.getConfigurationsBelongingToTeam()).willReturn(configurationSet);
		 given(deploymentResourceMock.getApplicationName()).willReturn(applicationName);
		 given(deploymentResourceMock.getConfigurationName()).willReturn(configurationName);

		 HttpServletRequest request = new MockHttpServletRequest();
		 ResponseEntity responseEntity = new ResponseEntity<>("{}", null, HttpStatus.CREATED);

		 when(teamService.findByName(teamName)).thenReturn(team);
		 when(teamService.findSharedApplicationWitinTeam(team, applicationName)).thenCallRealMethod();
		 when(teamService.findSharedConfigurationWitinTeam(team, configurationName)).thenCallRealMethod();
		 when(subject.deployForTeamShared(request, principal, teamName, deploymentResourceMock)).thenCallRealMethod();
		 when(subject.addDeployment(request, principal, deploymentResourceMock)).thenReturn(responseEntity);
		 ResponseEntity<?> addedDeployment = subject.deployForTeamShared(request, principal, teamName, deploymentResourceMock);
		 assertNotNull(addedDeployment.getBody());
		 assertTrue(addedDeployment.getStatusCode().equals(HttpStatus.CREATED));
	 }

	 // run the flow of the add method and check no hiccups and deployment is created
	@Test
	public void test_add_deployment() throws InvalidApplicationInputValueException, ConfigurationNotUsableForApplicationException, CloudCredentialNotUsableForApplicationException, IOException {

		String sharedWithUsername = "sharedWithUsername";
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn(sharedWithUsername);


		//get account of the user with whom application and configuration are shared
		Account account = mock(Account.class);
		String accountReference = "accountReference";
		given(accountService.findByUsername(sharedWithUsername)).willReturn(account);
		given(account.getGivenName()).willReturn(sharedWithUsername);
		given(account.getUsername()).willReturn(sharedWithUsername);
		given(account.getFirstJoinedDate()).willReturn(new Date(0, 0, 0));
		given(account.getReference()).willReturn(accountReference);

		//get account of the user who owns application and configuration
		Account owner =  mock(Account.class);
		String username = "username";
		given(owner.getUsername()).willReturn(username);
		given(accountService.findByUsername(username)).willReturn(owner);
		given(owner.getGivenName()).willReturn(username);
		given(owner.getUsername()).willReturn(username);
		given(owner.getFirstJoinedDate()).willReturn(new Date(0, 0, 0));
		given(owner.getReference()).willReturn(accountReference);


		DeploymentResource input = mock(DeploymentResource.class);
		given(input.getConfigurationAccountUsername()).willReturn(username);
		given(input.getApplicationAccountUsername()).willReturn(username);
		String sshkey = "sshkey";
		given(input.getUserSshKey()).willReturn(sshkey);

		//team
		Team team = mock(Team.class);
		String teamName = "someTeamName";
		String domainReference = "some ref";
		given(team.getName()).willReturn(teamName);
		given(team.getDomainReference()).willReturn(domainReference);

		//application
		given(input.getApplicationAccountUsername()).willReturn(username);
		String applicationName = "applicationName";
		given(input.getApplicationName()).willReturn(applicationName);
		Application application = mock(Application.class);
		given(application.getName()).willReturn(applicationName);given(applicationService.findByAccountUsernameAndName(username,applicationName)).willReturn(application);

		//set up teams, sharedwith user is a member of only one of these teams
		Set<Account> teamAccounts = new HashSet<>();
		teamAccounts.add(account);
		teamAccounts.add(owner);
		given(team.getAccountsBelongingToTeam()).willReturn(teamAccounts);
		Set<Team> sharedWithTeams = new HashSet<>();
		sharedWithTeams.add(team);

		//application is shared not owned
		given(applicationService.findByAccountUsernameAndName(sharedWithUsername,applicationName))
		.willThrow(ApplicationNotFoundException.class);

		Set<Application> applications = new HashSet<>();
		applications.add(application);
		given(team.getApplicationsBelongingToTeam()).willReturn(applications);
		when(application.getSharedWithTeams()).thenReturn(sharedWithTeams);

		given(account.getMemberOfTeams()).willReturn(sharedWithTeams);
		given(applicationService.isApplicationSharedWithAccount(account, application)).willCallRealMethod();

		//configuration
		String configurationName = "config";
		Configuration config = mock(Configuration.class);
		when(config.getAccount()).thenReturn(owner);
		when(config.getSharedWithTeams()).thenReturn(sharedWithTeams);
		when(input.getConfigurationAccountUsername()).thenReturn(username);
		when(input.getConfigurationName()).thenReturn(configurationName);
		when(config.getName()).thenReturn(configurationName);
		when(configurationService.findByNameAndAccountUsername(input.getConfigurationName(), input.getConfigurationAccountUsername()))
		.thenReturn(config);
		when(config.getHardUsageLimit()).thenReturn(1.0);
		when(configurationService.getTotalConsumption(config, deploymentIndexService)).thenReturn(0.5);
		when(configurationService.canConfigurationBeUsedForApplication(config, application, account)).thenCallRealMethod();

		//cdp
		String cdpReference = "cdpReference";
		String cdpName = "cdpName";
		given(config.getConfigDeployParamsReference()).willReturn(cdpReference);
		ConfigDeploymentParamsCopy configDeploymentParamsCopy = mock(ConfigDeploymentParamsCopy.class);
		given(configDeploymentParamsCopy.getName()).willReturn(cdpName);
		given(configDeploymentParamsCopy.getConfigurationDeploymentParametersReference()).willReturn(cdpReference);
		given(configurationDeploymentParamsCopyService.findByConfigurationDeploymentParametersReference(cdpReference))
		.willReturn(configDeploymentParamsCopy);
		List<ConfigDeploymentParamsCopy> cdpCopyList = new ArrayList<>();
		cdpCopyList.add(configDeploymentParamsCopy);
		given(configurationDeploymentParamsCopyService.findByName(cdpName)).willReturn(cdpCopyList.get(0));
		given(configurationService.isConfigurationSharedWithAccount(account, config)).willCallRealMethod();
		given(configurationDeploymentParamsCopyService.findByConfigurationDeploymentParametersReference(cdpReference))
		.willReturn(configDeploymentParamsCopy);

		//assigned cloud provider parameters
		String cloudProviderParametersName = "cppName";
		String cloudProviderParametersReference= "cppReference";
		config.cloudProviderParametersName = cloudProviderParametersName;
		config.setCloudProviderParametersName(cloudProviderParametersName);
		when(config.getCloudProviderParametersName()).thenReturn(cloudProviderParametersName);
		when(config.getCloudProviderParametersReference()).thenReturn(cloudProviderParametersReference);
		when(cloudProviderParameters.getName()).thenReturn(cloudProviderParametersName);
		CloudProviderParameters selectedCloudProviderParameters = mock(CloudProviderParameters.class);

		given(selectedCloudProviderParameters.getReference()).willReturn(cloudProviderParametersReference);
		String cloudProvider = "ostack";
		given(selectedCloudProviderParameters.getCloudProvider()).willReturn(cloudProvider);
		given(cloudProviderParametersService.isCloudProviderParametersSharedWithAccount(account, selectedCloudProviderParameters)).willCallRealMethod();
		given(cloudProviderParametersService.findByReference(cloudProviderParametersReference)).willReturn(selectedCloudProviderParameters);
		given(cloudProviderParametersService.checkForMatchingTeam(isA(List.class),isA(List.class))).willCallRealMethod();
		given(selectedCloudProviderParameters.getAccount()).willReturn(owner);
		//application cloud providers
		Collection<ApplicationCloudProvider> acpList = new ArrayList<>();
		ApplicationCloudProvider acp = mock(ApplicationCloudProvider.class);
		given(acp.getName()).willReturn("somename");
		given(acp.getPath()).willReturn("somepath");
		acpList.add(acp);
		given(application.getCloudProviders()).willReturn(acpList);
		given(selectedCloudProviderParameters.getName()).willReturn(cloudProviderParametersName);

		//cloud provider parameters copy
		CloudProviderParamsCopy cppCopy = mock(CloudProviderParamsCopy.class);
		when(cloudProviderParametersService.findByReference(cloudProviderParametersReference)).thenReturn(selectedCloudProviderParameters);
		given(cppCopy.getAccount()).willReturn(owner);
		cppCopy.account = owner;
		given(cppCopy.getCloudProviderParametersReference()).willReturn(cloudProviderParametersReference);
		CloudProviderParametersCopyResource cppCopyResource = new CloudProviderParametersCopyResource(cppCopy);
		when(input.getCloudProviderParametersCopy()).thenReturn(cppCopyResource);
		given(cloudProviderParametersCopyService.findByCloudProviderParametersReference(cloudProviderParametersReference))
		.willReturn(cppCopy);

		//deployment application
		DeploymentApplication deploymentApplication = mock(DeploymentApplication.class);
		given(deploymentApplicationService.createDeploymentApplication(application)).willReturn(deploymentApplication);
		given(deploymentApplication.getName()).willReturn(applicationName);
		given(deploymentApplication.getAccount()).willReturn(account);
		Collection<DeploymentApplicationCloudProvider> dacpList = new ArrayList<>();
		given(deploymentApplicationService.save(deploymentApplication)).willReturn(deploymentApplication);

		// application inputs 
		Collection<DeploymentAssignedInputResource> inputResources = new ArrayList<>();
		DeploymentAssignedInputResource inputResource = mock(DeploymentAssignedInputResource.class);
		when(inputResource.getInputName()).thenReturn("somename");
		when(inputResource.getAssignedValue()).thenReturn("somevalue");

		inputResources.add(inputResource);
		when(input.getAssignedInputs()).thenReturn(inputResources);

		Deployment deployment = mock(Deployment.class);
		given(deployment.getId()).willReturn(1l);
		given(deploymentService.save(isA(Deployment.class))).willReturn(deployment);
		given(deployment.getAccount()).willReturn(account);
		given(deployment.getDeploymentApplication()).willReturn(deploymentApplication);
		HttpServletRequest request = new MockHttpServletRequest();
		when(subject.addDeployment(request, principal, input)).thenCallRealMethod();
		ResponseEntity<?> addedDeployment = subject.addDeployment(request, principal, input);
		assertNotNull(addedDeployment.getBody());
		assertTrue(addedDeployment.getStatusCode().equals(HttpStatus.CREATED));
		assertTrue(application.getCloudProviders().containsAll(deploymentApplication.getCloudProviders()));
		assertTrue(deployment.getDeploymentApplication().equals(deploymentApplication));
	}

	@Test(expected = InvalidConfigurationInputException.class)
	public void configuration_name_not_specified() throws InvalidApplicationInputValueException,
			ConfigurationNotUsableForApplicationException, CloudCredentialNotUsableForApplicationException, IOException {

		String username = "username";
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn(username);

		DeploymentResource input = mock(DeploymentResource.class);
		given(input.getConfigurationName()).willReturn(null);
		given(input.getApplicationAccountUsername()).willReturn(username);

		//get account
		Account account = mock(Account.class);
		given(accountService.findByUsername(username)).willReturn(account);
		given(account.getGivenName()).willReturn(username);
		given(account.getUsername()).willReturn(username);

		//application
		String applicationName = "applicationName";
		given(input.getApplicationName()).willReturn(applicationName);
		Application application = mock(Application.class);
		given(application.getName()).willReturn(applicationName);
		given(application.getAccount()).willReturn(account);
		given(applicationService.findByAccountUsernameAndName(username,applicationName)).willReturn(application);
		HttpServletRequest request = new MockHttpServletRequest();
		when(subject.addDeployment(request, principal, input)).thenCallRealMethod();
		subject.addDeployment(request, principal, input);
	}


	@Test(expected = InvalidConfigurationInputException.class)
	public void configuration_owner_name_not_specified() throws InvalidApplicationInputValueException,
			ConfigurationNotUsableForApplicationException, CloudCredentialNotUsableForApplicationException, IOException {

		String username = "username";
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn(username);

		DeploymentResource input = mock(DeploymentResource.class);
		given(input.getConfigurationName()).willReturn("somename");
		given(input.getApplicationAccountUsername()).willReturn(username);
		given(input.getConfigurationAccountUsername()).willReturn(null);

		//get account
		Account account = mock(Account.class);
		given(accountService.findByUsername(username)).willReturn(account);
		given(account.getGivenName()).willReturn(username);
		given(account.getUsername()).willReturn(username);

		//application
		String applicationName = "applicationName";
		given(input.getApplicationName()).willReturn(applicationName);
		Application application = mock(Application.class);
		given(application.getName()).willReturn(applicationName);
		given(application.getAccount()).willReturn(account);
		given(applicationService.findByAccountUsernameAndName(username,applicationName)).willReturn(application);
		HttpServletRequest request = new MockHttpServletRequest();
		when(subject.addDeployment(request, principal, input)).thenCallRealMethod();
		ResponseEntity<?> addedDeployment = subject.addDeployment(request, principal, input);
	}

	@Test(expected = InvalidApplicationInputException.class)
	public void invalid_application_input_no_app_name() throws IOException,	InvalidApplicationInputValueException, ConfigurationNotUsableForApplicationException, CloudCredentialNotUsableForApplicationException{

		String username = "username";
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn(username);

		DeploymentResource input = mock(DeploymentResource.class);
		given(input.getApplicationName()).willReturn(null);
		HttpServletRequest request = new MockHttpServletRequest();
		when(subject.addDeployment(request, principal, input)).thenCallRealMethod();
		ResponseEntity<?> addedDeployment = subject.addDeployment(request, principal, input);
	}

	@Test(expected = InvalidApplicationInputException.class)
	public void invalid_application_input_no_app_owner_acc_username() throws InvalidApplicationInputValueException,
			ConfigurationNotUsableForApplicationException, CloudCredentialNotUsableForApplicationException, IOException {

		String username = "username";
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn(username);

		DeploymentResource input = mock(DeploymentResource.class);
		given(input.getApplicationAccountUsername()).willReturn(null);
		HttpServletRequest request = new MockHttpServletRequest();
		when(subject.addDeployment(request, principal, input)).thenCallRealMethod();
		ResponseEntity<?> addedDeployment = subject.addDeployment(request, principal, input);
	}

	@Test(expected=ApplicationNotFoundException.class)
	public void app_not_found_exception() throws IOException, InvalidApplicationInputValueException, ConfigurationNotUsableForApplicationException, CloudCredentialNotUsableForApplicationException{

		String username = "username";
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn(username);

		DeploymentResource input = mock(DeploymentResource.class);
		given(input.getConfigurationName()).willReturn("somename");
		given(input.getConfigurationAccountUsername()).willReturn(username);
		given(input.getApplicationAccountUsername()).willReturn(username);

		//get account
		Account account = mock(Account.class);
		given(accountService.findByUsername(username)).willReturn(account);
		given(account.getGivenName()).willReturn(username);
		given(account.getUsername()).willReturn(username);

		//application
		String applicationName = "applicationName";
		given(input.getApplicationName()).willReturn(applicationName);
		Application application = mock(Application.class);
		given(application.getName()).willReturn(applicationName);
		given(application.getAccount()).willReturn(account);
		given(applicationService.findByAccountUsernameAndName(username,applicationName)).willThrow(ApplicationNotFoundException.class);
		HttpServletRequest request = new MockHttpServletRequest();
		when(subject.addDeployment(request, principal, input)).thenCallRealMethod();
		subject.addDeployment(request, principal, input);

	}

	private Deployment deployment(String reference) {
		Deployment mockDeployment = mock(Deployment.class);
		when(mockDeployment.getReference()).thenReturn(reference);
		CloudProviderParameters mockCloudProviderParameters = mock(CloudProviderParameters.class);
		DeploymentApplication mockDeploymentApp = mock(DeploymentApplication.class);
		when(mockDeploymentApp.getRepoPath()).thenReturn("irrelevant");
		when(mockDeployment.getDeploymentApplication()).thenReturn(mockDeploymentApp);
		Account mockAccount = mock(Account.class);
		when(mockAccount.getUsername()).thenReturn(A_USER_NAME);
		when(mockDeployment.getAccount()).thenReturn(mockAccount);
		when(deploymentService.findByReference(reference)).thenReturn(mockDeployment);
		when(mockDeployment.getCloudProviderParametersReference()).thenReturn(cppReference);
		DeploymentConfiguration deploymentConfiguration = mock(DeploymentConfiguration.class);
		when(mockDeployment.getDeploymentConfiguration()).thenReturn(deploymentConfiguration);
		when(deploymentConfiguration.getName()).thenReturn("some string");
		when(deploymentConfiguration.getConfigDeploymentParametersReference()).thenReturn(reference);
		when(cloudProviderParametersService.findByReference(Mockito.anyString())).thenReturn(mockCloudProviderParameters);
		when(mockCloudProviderParameters.getAccount()).thenReturn(mockAccount);
		java.sql.Date date = mock(java.sql.Date.class);
		when(mockAccount.getFirstJoinedDate()).thenReturn(date);
		when(mockAccount.getReference()).thenReturn("some ref");
		when(mockAccount.getGivenName()).thenReturn("given name");
		ConfigurationDeploymentParameters cdps = mock(ConfigurationDeploymentParameters.class);
		when(configurationDeploymentParametersService.findByReference(reference)).thenReturn(cdps);
		when(cdps.getReference()).thenReturn(reference);
		return mockDeployment;
	}

	@Test
	public void baseUrl() throws Exception {

		/*
            Portal Dev          https://dev.api.portal.tsi.ebi.ac.uk
            Portal Master       https://api.portal.tsi.ebi.ac.uk
            Local Deployment    http://localhost:8080

            With server path    https://api.portal.tsi.ebi.ac.uk/deployments/TSI000000000000001/stopme

		 */
		MockHttpServletRequest localURLRequest  = mockRequest("localhost", 8080);
		when(subject.baseURL(localURLRequest)).thenCallRealMethod();
		when(subject.getPortStr(Mockito.any(URL.class))).thenCallRealMethod();
		assertEquals("http://localhost:8080", subject.baseURL(localURLRequest));
		MockHttpServletRequest devURLRequest = mockRequest("dev.api.portal.tsi.ebi.ac.uk");
		when(subject.baseURL( devURLRequest)).thenCallRealMethod();
		assertEquals( "http://dev.api.portal.tsi.ebi.ac.uk" , subject.baseURL( devURLRequest) );
		MockHttpServletRequest prodURLRequest = mockRequest("api.portal.tsi.ebi.ac.uk", -1, "/deployments/TSI000000000000001/stopme");
		when(subject.baseURL( prodURLRequest)).thenCallRealMethod();
		assertEquals( "http://api.portal.tsi.ebi.ac.uk"     , subject.baseURL(prodURLRequest));
	}

	MockHttpServletRequest mockRequest(String host)            {  return mockRequest(host, -1);	          }
	MockHttpServletRequest mockRequest(String host, int port)  {  return mockRequest(host, port, null);   }

	MockHttpServletRequest mockRequest(String host, int port, String path) {

		MockHttpServletRequest request = new MockHttpServletRequest();

		if (path != null)
			request.setRequestURI(path);

		if (port != -1)
			request.setServerPort(port);

		request.setServerName(host);

		return request;
	}

	@Test(expected = ConfigurationNotUsableForApplicationException.class)
	public void testIfConfigurationisNotUsable() throws InvalidApplicationInputValueException, IOException,ConfigurationNotUsableForApplicationException, CloudCredentialNotUsableForApplicationException {

		DeploymentResource input = mock(DeploymentResource.class);
		
		String sharedWithUsername = "sharedWithUsername";
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn(sharedWithUsername);
		//get account of the user with whom application and configuration are shared
		Account account = mock(Account.class);
		String accountReference = "accountReference";
		given(accountService.findByUsername(sharedWithUsername)).willReturn(account);
		given(account.getGivenName()).willReturn(sharedWithUsername);
		given(account.getUsername()).willReturn(sharedWithUsername);
		given(account.getFirstJoinedDate()).willReturn(new Date(0, 0, 0));
		given(account.getReference()).willReturn(accountReference);
		
		
		String applicationName = "applicationName";
		String applicationOwnerAccountUsername = "applicationOwnerAccountUsername";
		Account applicationOwner =  mock(Account.class);
		when(input.getApplicationName()).thenReturn(applicationName);
		when(input.getApplicationAccountUsername()).thenReturn(applicationOwnerAccountUsername);
		given(accountService.findByUsername(applicationOwnerAccountUsername)).willReturn(applicationOwner);
		Application application = mock(Application.class);
		given(application.getName()).willReturn(applicationName);
		given(applicationOwner.getUsername()).willReturn(applicationOwnerAccountUsername);
		given(applicationService.findByAccountUsernameAndName(applicationOwnerAccountUsername, applicationName)).willReturn(application);
		given(applicationService.isApplicationSharedWithAccount(account, application)).willReturn(true);
		
		String configurationName = "configurationName";
		String configurationOwnerAccountUsername = "configurationOwnerAccountUsername";
		Account configurationOwner =  mock(Account.class);
		when(input.getConfigurationName()).thenReturn(configurationName);
		when(input.getConfigurationAccountUsername()).thenReturn(configurationOwnerAccountUsername);
		given(accountService.findByUsername(configurationOwnerAccountUsername)).willReturn(configurationOwner);
		Configuration configuration = mock(Configuration.class);
		given(configuration.getName()).willReturn(configurationName);
		given(configurationOwner.getUsername()).willReturn(configurationOwnerAccountUsername);
		given(configurationService.findByNameAndAccountUsername(configurationName, sharedWithUsername)).willThrow(ConfigurationNotFoundException.class);
		given(configurationService.findByNameAndAccountUsername(configurationName, configurationOwnerAccountUsername)).willReturn(configuration);
		given(configurationService.isConfigurationSharedWithAccount(account, configuration)).willReturn(true);	
		given(configurationService
				.canConfigurationBeUsedForApplication(
						isA(Configuration.class),
						isA(Application.class),
						isA(Account.class))).willReturn(false);
		
		HttpServletRequest request = new MockHttpServletRequest();
		when(subject.addDeployment(request, principal, input)).thenCallRealMethod();
		subject.addDeployment(request, principal, input);

	}
	
	@Test(expected = ConfigurationNotUsableForApplicationException.class)
	public void testIfConfigurationisNotShared() throws InvalidApplicationInputValueException,
	IOException, ConfigurationNotUsableForApplicationException, CloudCredentialNotUsableForApplicationException {

		DeploymentResource input = mock(DeploymentResource.class);
		
		String sharedWithUsername = "sharedWithUsername";
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn(sharedWithUsername);
		//get account of the user with whom application and configuration are shared
		Account account = mock(Account.class);
		String accountReference = "accountReference";
		given(accountService.findByUsername(sharedWithUsername)).willReturn(account);
		given(account.getGivenName()).willReturn(sharedWithUsername);
		given(account.getUsername()).willReturn(sharedWithUsername);
		given(account.getFirstJoinedDate()).willReturn(new Date(0, 0, 0));
		given(account.getReference()).willReturn(accountReference);
		
		
		String applicationName = "applicationName";
		String applicationOwnerAccountUsername = "applicationOwnerAccountUsername";
		Account applicationOwner =  mock(Account.class);
		when(input.getApplicationName()).thenReturn(applicationName);
		when(input.getApplicationAccountUsername()).thenReturn(applicationOwnerAccountUsername);
		given(accountService.findByUsername(applicationOwnerAccountUsername)).willReturn(applicationOwner);
		Application application = mock(Application.class);
		given(application.getName()).willReturn(applicationName);
		given(applicationOwner.getUsername()).willReturn(applicationOwnerAccountUsername);
		given(applicationService.findByAccountUsernameAndName(applicationOwnerAccountUsername, applicationName)).willReturn(application);
		given(applicationService.isApplicationSharedWithAccount(account, application)).willReturn(true);
		
		String configurationName = "configurationName";
		String configurationOwnerAccountUsername = "configurationOwnerAccountUsername";
		Account configurationOwner =  mock(Account.class);
		when(input.getConfigurationName()).thenReturn(configurationName);
		when(input.getConfigurationAccountUsername()).thenReturn(configurationOwnerAccountUsername);
		given(accountService.findByUsername(configurationOwnerAccountUsername)).willReturn(configurationOwner);
		Configuration configuration = mock(Configuration.class);
		given(configuration.getName()).willReturn(configurationName);
		given(configurationOwner.getUsername()).willReturn(configurationOwnerAccountUsername);
		given(configurationService.findByNameAndAccountUsername(configurationName, sharedWithUsername)).willThrow(ConfigurationNotFoundException.class);
		given(configurationService.findByNameAndAccountUsername(configurationName, configurationOwnerAccountUsername)).willReturn(configuration);
		given(configurationService.isConfigurationSharedWithAccount(account, configuration)).willReturn(false);	
		HttpServletRequest request = new MockHttpServletRequest();
		when(subject.addDeployment(request, principal, input)).thenCallRealMethod();
		subject.addDeployment(request, principal, input);

	}
	
	@Test(expected = ConfigurationNotUsableForApplicationException.class)
	public void testIfCredentialisNotUsable() throws ConfigurationNotUsableForApplicationException, CloudCredentialNotUsableForApplicationException, IOException {

		DeploymentResource input = mock(DeploymentResource.class);
		
		String sharedWithUsername = "sharedWithUsername";
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn(sharedWithUsername);
		//get account of the user with whom application and configuration are shared
		Account account = mock(Account.class);
		String accountReference = "accountReference";
		given(accountService.findByUsername(sharedWithUsername)).willReturn(account);
		given(account.getGivenName()).willReturn(sharedWithUsername);
		given(account.getUsername()).willReturn(sharedWithUsername);
		given(account.getFirstJoinedDate()).willReturn(new Date(0, 0, 0));
		given(account.getReference()).willReturn(accountReference);
		
		
		String applicationName = "applicationName";
		String applicationOwnerAccountUsername = "applicationOwnerAccountUsername";
		Account applicationOwner =  mock(Account.class);
		when(input.getApplicationName()).thenReturn(applicationName);
		when(input.getApplicationAccountUsername()).thenReturn(applicationOwnerAccountUsername);
		given(accountService.findByUsername(applicationOwnerAccountUsername)).willReturn(applicationOwner);
		Application application = mock(Application.class);
		given(application.getName()).willReturn(applicationName);
		given(applicationOwner.getUsername()).willReturn(applicationOwnerAccountUsername);
		given(applicationService.findByAccountUsernameAndName(applicationOwnerAccountUsername, applicationName)).willReturn(application);
		given(applicationService.isApplicationSharedWithAccount(account, application)).willReturn(true);
		
		String configurationName = "configurationName";
		when(input.getConfigurationName()).thenReturn(configurationName);
		when(input.getConfigurationAccountUsername()).thenReturn(sharedWithUsername);
		given(accountService.findByUsername(sharedWithUsername)).willReturn(account);
		Configuration configuration = mock(Configuration.class);
		given(configuration.getName()).willReturn(configurationName);
		String credentialName = "credentialName";
		given(configuration.getCloudProviderParametersName()).willReturn(credentialName);
		given(configurationService.findByNameAndAccountUsername(configurationName, sharedWithUsername)).willReturn(configuration);
			
		given(cloudProviderParametersService.findByNameAndAccountUsername(credentialName, sharedWithUsername))
		.willThrow(CloudProviderParametersNotFoundException.class);
		CloudProviderParameters cloudProviderParameters =  mock(CloudProviderParameters.class);
		String credentialReference = "credentialReference";
		given(configuration.getCloudProviderParametersReference()).willReturn(credentialReference);
		given(cloudProviderParametersService.findByReference(credentialReference)).willReturn(cloudProviderParameters);
		
		//credential owner account
		Account cppOwner = mock(Account.class);
		String cppOwnerName = "cppOwnerName";
		given(cppOwner.getUsername()).willReturn(cppOwnerName);
		given(cloudProviderParameters.getAccount()).willReturn(cppOwner);
		given(accountService.findByUsername(cppOwnerName)).willReturn(cppOwner);
		given(cloudProviderParametersService
				.isCloudProviderParametersSharedWithAccount(account, cloudProviderParameters)).willReturn(true);	
		given(cloudProviderParametersService
				.canCredentialBeUsedForApplication(
						isA(CloudProviderParameters.class),
						isA(Application.class),
						isA(Account.class))).willReturn(false);
		HttpServletRequest request = new MockHttpServletRequest();
		when(subject.addDeployment(request, principal, input)).thenCallRealMethod();
		subject.addDeployment(request, principal, input);

	}
	
	@Test(expected = ConfigurationNotUsableForApplicationException.class)
	public void testIfCredentialisNotShared() throws InvalidApplicationInputValueException, IOException,
	ConfigurationNotUsableForApplicationException, CloudCredentialNotUsableForApplicationException {

		DeploymentResource input = mock(DeploymentResource.class);
		
		String sharedWithUsername = "sharedWithUsername";
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn(sharedWithUsername);
		//get account of the user with whom application and configuration are shared
		Account account = mock(Account.class);
		String accountReference = "accountReference";
		given(accountService.findByUsername(sharedWithUsername)).willReturn(account);
		given(account.getGivenName()).willReturn(sharedWithUsername);
		given(account.getUsername()).willReturn(sharedWithUsername);
		given(account.getFirstJoinedDate()).willReturn(new Date(0, 0, 0));
		given(account.getReference()).willReturn(accountReference);
		
		
		String applicationName = "applicationName";
		String applicationOwnerAccountUsername = "applicationOwnerAccountUsername";
		Account applicationOwner =  mock(Account.class);
		when(input.getApplicationName()).thenReturn(applicationName);
		when(input.getApplicationAccountUsername()).thenReturn(applicationOwnerAccountUsername);
		given(accountService.findByUsername(applicationOwnerAccountUsername)).willReturn(applicationOwner);
		Application application = mock(Application.class);
		given(application.getName()).willReturn(applicationName);
		given(applicationOwner.getUsername()).willReturn(applicationOwnerAccountUsername);
		given(applicationService.findByAccountUsernameAndName(applicationOwnerAccountUsername, applicationName)).willReturn(application);
		given(applicationService.isApplicationSharedWithAccount(account, application)).willReturn(true);
		
		String configurationName = "configurationName";
		when(input.getConfigurationName()).thenReturn(configurationName);
		when(input.getConfigurationAccountUsername()).thenReturn(sharedWithUsername);
		given(accountService.findByUsername(sharedWithUsername)).willReturn(account);
		Configuration configuration = mock(Configuration.class);
		given(configuration.getName()).willReturn(configurationName);
		String credentialName = "credentialName";
		given(configuration.getCloudProviderParametersName()).willReturn(credentialName);
		given(configurationService.findByNameAndAccountUsername(configurationName, sharedWithUsername)).willReturn(configuration);
			
		given(cloudProviderParametersService.findByNameAndAccountUsername(credentialName, sharedWithUsername))
		.willThrow(CloudProviderParametersNotFoundException.class);
		CloudProviderParameters cloudProviderParameters =  mock(CloudProviderParameters.class);
		String credentialReference = "credentialReference";
		given(configuration.getCloudProviderParametersReference()).willReturn(credentialReference);
		given(cloudProviderParametersService.findByReference(credentialReference)).willReturn(cloudProviderParameters);
		
		//credential owner account
		Account cppOwner = mock(Account.class);
		String cppOwnerName = "cppOwnerName";
		given(cppOwner.getUsername()).willReturn(cppOwnerName);
		given(cloudProviderParameters.getAccount()).willReturn(cppOwner);
		given(accountService.findByUsername(cppOwnerName)).willReturn(cppOwner);
		given(cloudProviderParametersService
				.canCredentialBeUsedForApplication(
						isA(CloudProviderParameters.class),
						isA(Application.class),
						isA(Account.class))).willReturn(false);
		HttpServletRequest request = new MockHttpServletRequest();
		when(subject.addDeployment(request, principal, input)).thenCallRealMethod();
		subject.addDeployment(request, principal, input);

	}
	
}