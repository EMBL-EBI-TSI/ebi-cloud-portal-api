package uk.ac.ebi.tsc.portal.api.deployment.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.service.AccountService;
import uk.ac.ebi.tsc.portal.api.application.controller.InvalidApplicationInputValueException;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.application.service.ApplicationService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParametersService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyService;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigDeploymentParamsCopy;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigDeploymentParamsCopyService;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationDeploymentParametersService;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationService;
import uk.ac.ebi.tsc.portal.api.deployment.repo.*;
import uk.ac.ebi.tsc.portal.api.deployment.service.CloudCredentialNotUsableForApplicationException;
import uk.ac.ebi.tsc.portal.api.deployment.service.ConfigurationNotUsableForApplicationException;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentApplicationService;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.volumeinstance.service.VolumeInstanceService;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDeployer;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDeployerHelper;
import uk.ac.ebi.tsc.portal.usage.deployment.service.DeploymentIndexService;
import uk.ac.ebi.tsc.portal.usage.tracker.DeploymentStatusTracker;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
// Initializes mocks annotated with @Mock objects.Mocks are initialized before each test method
public class DeploymentRestcontrollerUnitTest {

    private String username = "userfoo";
    private String cloudProvider = "OSTACK";

    private String accountReference = "accountReference";
    private String configurationName = "config";
    private String cdpReference = "cdpReference";
    private String cdpName = "cdpName";

    private String cloudProviderParametersName = "cppName";
    private String cloudProviderParametersReference = "cppReference";

    private String depReference = "dep01";
    private String name = "Foo";
    private String password = "A password";
    private String email = "an@email.com";
    private String sshkey = "sshkey";
    private Date date = new Date(new java.util.Date().getTime());
    private String avatarUrl = "some_url";
    private String organization = "An organisation";

    private String applicationName = "APP";
    private String applicationReference = "APP_REF";
    private String repo_path = "path";
    private Double total_consumption = 0.5;
    private String requestURL = "http://localhost:8080/";

    private DeploymentRestController deploymentRestController;

    private Account userAccount;

    private CloudProviderParameters cloudProviderParameters;

    private ConfigDeploymentParamsCopy configDeploymentParamsCopy;

    private ConfigurationDeploymentParameters configurationDeploymentParameters;

    private Application application1;

    private Configuration userConfig;

    @Mock
    CloudProviderParametersService cloudProviderParametersService;

    @Mock
    CloudProviderParamsCopyService cloudProviderParametersCopyService;

    @Mock
    ConfigurationDeploymentParametersService configurationDeploymentParametersService;

    @Mock
    DeploymentService deploymentService;

    @Mock
    DeploymentStatusTracker deploymentStatusTracker;

    @Mock
    ConfigurationService configurationService;

    @Mock
    AccountService accountService;

    @Mock
    ApplicationService applicationService;

    @Mock
    ConfigDeploymentParamsCopyService configDeploymentParamsCopyService;

    @Mock
    DeploymentIndexService deploymentIndexService;

    @Mock
    DeploymentApplicationService deploymentApplicationService;

    @Mock
    ApplicationDeployer applicationDeployer;

    @Mock
    ApplicationDeployerHelper applicationDeployerHelper;

    @Mock
    VolumeInstanceService volumeInstanceService;

    @Mock
    HttpServletRequest request;

    @Mock
    Principal principal;


    @Before
    public void setUp() {
        // Setting up required objects for the deployment
        deploymentRestController = new DeploymentRestController(deploymentService, accountService, applicationService, volumeInstanceService,
                cloudProviderParametersService, configurationService, null, applicationDeployer, deploymentStatusTracker,
                configurationDeploymentParametersService, null, null, deploymentApplicationService,
                cloudProviderParametersCopyService, configDeploymentParamsCopyService, null, null,
                null, applicationDeployerHelper);
        Properties props = new Properties();
        props.put("be.applications.root", "blah");
        props.put("be.deployments.root", "bleh");
        deploymentRestController.setProperties(props);
        userAccount = new Account(accountReference, username, name, password, email, date, organization, avatarUrl);
        application1 = new Application("", repo_path, applicationName, applicationReference, userAccount);
        configurationDeploymentParameters = new ConfigurationDeploymentParameters("CDP", userAccount);
        configurationDeploymentParameters.setReference(cdpReference);
        configDeploymentParamsCopy = new ConfigDeploymentParamsCopy(configurationDeploymentParameters);
        cloudProviderParameters = new CloudProviderParameters(cloudProviderParametersName, cloudProvider, userAccount);
        userConfig = new Configuration(configurationName, userAccount, cloudProviderParametersName, cloudProviderParametersReference, "",
                0d, 1.0, configDeploymentParamsCopy);
        userConfig.setConfigDeployParamsReference(cdpReference);

        // Controller needs RequestContextHolder and HttpServlet Request
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @Test
    public void add_deployment_by_own_application() throws InvalidApplicationInputValueException, ConfigurationNotUsableForApplicationException,
            CloudCredentialNotUsableForApplicationException, IOException {

        DeploymentApplication deploymentApplication = new DeploymentApplication(application1);
        CloudProviderParameters selectedCloudProviderParameters = new CloudProviderParameters(cloudProviderParametersName, cloudProvider, userAccount);

        given(principal.getName()).willReturn(username);
        when(accountService.findByUsername(username)).thenReturn(userAccount);
        // Return application1 for the user - userfoo. application1 is owned by userfoo
        when(applicationService.findByAccountUsernameAndName(username, applicationName)).thenReturn(application1);
        // config is created by user - userfoo, so return userConfig
        when(configurationService.findByNameAndAccountUsername(configurationName, username)).thenReturn(userConfig);
        when(configurationService.getTotalConsumption(userConfig, deploymentIndexService)).thenReturn(total_consumption);
        // configuration can be used with application1 ,as application1 is owned by same user, so return true
        when(configurationService.canConfigurationBeUsedForApplication(userConfig, application1, userAccount)).thenReturn(true);
        when(configDeploymentParamsCopyService.findByConfigurationDeploymentParametersReference(cdpReference))
                .thenReturn(configDeploymentParamsCopy);
        when(configDeploymentParamsCopyService.findByName(cdpName)).thenReturn(configDeploymentParamsCopy);
        when(configurationService.isConfigurationSharedWithAccount(userAccount, userConfig)).thenCallRealMethod();
        when(configDeploymentParamsCopyService.findByConfigurationDeploymentParametersReference(cdpReference))
                .thenReturn(configDeploymentParamsCopy);
        when(cloudProviderParametersService.findByReference(cloudProviderParametersReference)).thenReturn(selectedCloudProviderParameters);
        when(cloudProviderParametersService.canCredentialBeUsedForApplication(selectedCloudProviderParameters, application1, userAccount)).thenCallRealMethod();
        given(request.getRequestURL()).willReturn(new StringBuffer(requestURL));
        when(cloudProviderParametersService.findByReference(cloudProviderParametersReference)).thenReturn(cloudProviderParameters);
        when(configurationDeploymentParametersService.findByReference(cdpReference)).thenReturn(configurationDeploymentParameters);
        //Return deploymentApplication for application1
        when(deploymentApplicationService.createDeploymentApplication(application1)).thenReturn(deploymentApplication);
        when(deploymentApplicationService.save(isA(DeploymentApplication.class))).thenReturn(deploymentApplication);

        DeploymentConfiguration deploymentConfiguration = new DeploymentConfiguration("config", username, sshkey, null,
                "", "");
        // Create deployment from previously created objects and references
        Deployment deployment = deployment(depReference, cdpReference, userAccount, deploymentApplication, deploymentConfiguration);
        // Set some variables through constructor
        DeploymentResource input = new DeploymentResource(deployment, null);
        input.setConfigurationName(configurationName);
        input.setConfigurationAccountUsername(username);
        input.setApplicationAccountUsername(username);
        input.setUserSshKey(sshkey);
        input.setApplicationAccountUsername(username);

        // Assigned inputs for the Deployment
        String inputName = "input";
        String value = "value";
        DeploymentAssignedInputResource inputResource = mock(DeploymentAssignedInputResource.class);
        when(inputResource.getInputName()).thenReturn(inputName);
        when(inputResource.getAssignedValue()).thenReturn(value);
        Collection<DeploymentAssignedInputResource> inputResources = new ArrayList<>();
        inputResources.add(inputResource);
        input.setAssignedInputs(inputResources);

        when(deploymentService.save(isA(Deployment.class))).thenReturn(deployment);

        ResponseEntity<?> addedDeployment = deploymentRestController.addDeployment(request, principal, input);
        assertNotNull(addedDeployment.getBody());
        assertTrue(addedDeployment.getStatusCode().equals(HttpStatus.CREATED));
        assertTrue(application1.getCloudProviders().containsAll(deploymentApplication.getCloudProviders()));
        assertTrue(deployment.getDeploymentApplication().equals(deploymentApplication));
    }


    private Deployment deployment(String depReference, String  cloudProviderParametersReference, Account userAccount,
                                  DeploymentApplication depApplication,DeploymentConfiguration depConfiguration) {

        Deployment deployment = new Deployment(depReference, userAccount, depApplication, cloudProviderParametersReference, sshkey);
        deployment.setDeploymentConfiguration(depConfiguration);
        deployment.setDeploymentApplication(depApplication);
        when(deploymentService.findByReference(depReference)).thenReturn(deployment);
        return deployment;
    }
}
