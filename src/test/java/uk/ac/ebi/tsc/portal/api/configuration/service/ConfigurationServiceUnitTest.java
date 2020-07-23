package uk.ac.ebi.tsc.portal.api.configuration.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopy;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParametersService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyService;
import uk.ac.ebi.tsc.portal.api.configuration.controller.ConfigurationResource;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigDeploymentParamsCopy;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationRepository;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;
import uk.ac.ebi.tsc.portal.api.utils.SendMail;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationServiceUnitTest {

    Account account = new Account("reference", "username", "givenName", "password", "email",
            new java.sql.Date(2000000000), "organisation", "avatarImageUrl");

    String cloudProvider = "openstack";

    //first configuration
    String cdp1Name = "cdp1";
    ConfigurationDeploymentParameters cdp1;
    ConfigDeploymentParamsCopy configDeploymentParamsCopy1;
    String cpp1Name = "cpp1";
    CloudProviderParamsCopy cppCopy1;
    CloudProviderParameters cpp1;
    String cppReference1 = "myCppReference1";
    String cdpReference1 = "myCdpReference1";
    Configuration configuration1;

    //second configuration
    String cdp2Name = "cdp2";
    ConfigurationDeploymentParameters cdp2;
    ConfigDeploymentParamsCopy configDeploymentParamsCopy2;
    String cpp2Name = "cpp2";
    CloudProviderParamsCopy cppCopy2;
    CloudProviderParameters cpp2;
    String cppReference2 = "myCppReference2";
    String cdpReference2 = "myCdpReference2";
    Configuration configuration2;

    List configurationList = new ArrayList<Configuration>();

    @Mock
    ConfigurationRepository configurationRepository;

    @Mock
    private DomainService domainService;

    @Mock
    private CloudProviderParametersService cppService;

    @Mock
    private CloudProviderParamsCopyService cppCopyService;

    @Mock
    private ConfigurationDeploymentParametersService cdpService;

    @Mock
    private ConfigDeploymentParamsCopyService cdpCopyService;

    @Mock
    private DeploymentService deploymentService;

    @Mock
    private SendMail sendMail;

    List<ConfigurationResource> configResourceList = new ArrayList<>();
    ConfigurationService testCandidate;

    @Before
    public void setUp() {

        testCandidate = new ConfigurationService(configurationRepository, domainService,
                cppService, cdpService, cppCopyService, deploymentService, sendMail);

        Account account = new Account("reference", "username", "givenName", "password", "email",
                new java.sql.Date(2000000000), "organisation", "avatarImageUrl");

        //first configuration
        cdp1 = new ConfigurationDeploymentParameters(cdp1Name, account);
        configDeploymentParamsCopy1 = new ConfigDeploymentParamsCopy(cdp1);
        cppCopy1 = new  CloudProviderParamsCopy(cpp1Name, cloudProvider, account);
        cpp1 = new CloudProviderParameters(cpp1Name, cloudProvider, account);
        configuration1 = new Configuration("myConfig", account,
                cppCopy1.getName(), cppReference1, "myKey", 2.0, 5.0, configDeploymentParamsCopy1);

        //second configuration
        cdp2 = new ConfigurationDeploymentParameters(cdp2Name, account);
        configDeploymentParamsCopy2 = new ConfigDeploymentParamsCopy(cdp2);
        cppCopy2 = new  CloudProviderParamsCopy(cpp2Name, cloudProvider, account);
        cpp2 = new CloudProviderParameters(cpp2Name, cloudProvider, account);
        configuration2 = new Configuration("myConfig", account,
                cppCopy2.getName(), cppReference2, "myKey", 2.0, 5.0, configDeploymentParamsCopy2);


        cpp1.setReference(cppReference1);
        cpp2.setReference(cppReference2);
        cppCopy1.setCloudProviderParametersReference(cppReference1);
        cppCopy2.setCloudProviderParametersReference(cppReference2);
        cdp1.setReference(cdpReference1);
        cpp2.setReference(cppReference2);



        configurationList = new ArrayList<Configuration>();

        // Controller needs RequestContextHolder and HttpServlet Request
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @Test
    public void testCheckObsoleteConfigurationsNoneOwnedConfiguration() {


        configurationList.add(configuration1);
        configurationList.add(configuration2);

        ConfigurationResource configurationResource1 = new ConfigurationResource(configuration1, cppCopy1);
        ConfigurationResource configurationResource2 = new ConfigurationResource(configuration2, cppCopy2);
        configurationResource1.setConfigDeploymentParametersReference(cdpReference1);
        configurationResource2.setConfigDeploymentParametersReference(cdpReference2);
        configResourceList.add(configurationResource1);
        configResourceList.add(configurationResource2);

        given(cppCopyService.findByCloudProviderParametersReference(cppReference1)).willReturn(cppCopy1);
        given(cppCopyService.findByCloudProviderParametersReference(cppReference2)).willReturn(cppCopy2);
        given(cppService.findByReference(cppReference1)).willReturn(cpp1);
        given(cppService.findByReference(cppReference2)).willReturn(cpp2);
        given(cdpCopyService.findByConfigurationDeploymentParametersReference(cdpReference1)).willReturn(configDeploymentParamsCopy2);
        given(cdpCopyService.findByConfigurationDeploymentParametersReference(cdpReference2)).willReturn(configDeploymentParamsCopy2);

        List<ConfigurationResource> obsoleteConfigurationList = testCandidate.checkObsoleteConfigurations(configResourceList,
                account, cdpCopyService);

    }

}

