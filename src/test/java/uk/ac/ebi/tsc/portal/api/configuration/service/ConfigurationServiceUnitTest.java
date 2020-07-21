package uk.ac.ebi.tsc.portal.api.configuration.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.portal.BePortalApiApplication;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopy;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParametersService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyService;
import uk.ac.ebi.tsc.portal.api.configuration.controller.ConfigurationDeploymentParametersResource;
import uk.ac.ebi.tsc.portal.api.configuration.controller.ConfigurationResource;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigDeploymentParamsCopy;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationRepository;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.utils.SendMail;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationServiceUnitTest {

    Account account = new Account("reference", "username", "givenName", "password", "email",
            new java.sql.Date(2000000000), "organisation", "avatarImageUrl");

    //first configuration
    ConfigurationDeploymentParameters cdp = new ConfigurationDeploymentParameters("cdpName", account);
    ConfigDeploymentParamsCopy configDeploymentParamsCopy = new ConfigDeploymentParamsCopy(cdp);
    CloudProviderParamsCopy cppCopy1 = new  CloudProviderParamsCopy("cpp1", "openstack", account);
    String cppReference1 = "myCppReference1";
    Configuration configuration = new Configuration("myConfig", account,
            cppCopy1.getName(), cppReference1, "myKey", 2.0, 5.0, configDeploymentParamsCopy);

    //second configuration
    ConfigurationDeploymentParameters cdp2 = new ConfigurationDeploymentParameters("cdpName2", account);
    ConfigDeploymentParamsCopy configDeploymentParamsCopy2 = new ConfigDeploymentParamsCopy(cdp2);
    CloudProviderParamsCopy cppCopy2 = new  CloudProviderParamsCopy("cpp2", "openstack", account);
    String cppReference2 = "myCppReference2";
    Configuration configuration2 = new Configuration("myConfig", account,
            cppCopy2.getName(), cppReference2, "myKey", 2.0, 5.0, configDeploymentParamsCopy2);
    List configurationList = new ArrayList<Configuration>();

    @Mock
    private ConfigurationRepository configurationRepository;

    @Mock
    private DomainService domainService;

    @Mock
    private CloudProviderParametersService cppService;

    @Mock
    private ConfigurationDeploymentParametersService cdpService;

    @Mock
    private ConfigDeploymentParamsCopyService cdpCopyService;

    @Mock
    private CloudProviderParamsCopyService cloudProviderParametersCopyService;

    @Mock
    private DeploymentService deploymentService;

    @Mock
    private SendMail sendMail;

    List<ConfigurationResource> configResourceList = new ArrayList<>();
    ConfigurationResource configurationResource1 = new ConfigurationResource(configuration, cppCopy1);
    ConfigurationResource configurationResource2 = new ConfigurationResource(configuration2, cppCopy2);
    ConfigurationService testCandidate;

    @Before
    public void setUp() {
        testCandidate = new ConfigurationService(configurationRepository, domainService,
                cppService, cdpService, cloudProviderParametersCopyService, deploymentService, sendMail);

        // Controller needs RequestContextHolder and HttpServlet Request
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @Test
    public void testCheckObsoleteConfigurations() {
        configurationList.add(configuration);
        configurationList.add(configuration2);
        configResourceList.add(configurationResource1);
        configResourceList.add(configurationResource2);
        List<ConfigurationResource> obsoleteConfigurationList = testCandidate.checkObsoleteConfigurations(configResourceList,
                account, cdpCopyService);

    }

}
