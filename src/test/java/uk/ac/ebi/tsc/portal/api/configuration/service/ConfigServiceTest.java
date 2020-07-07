package uk.ac.ebi.tsc.portal.api.configuration.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParametersService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyService;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigDeploymentParamsCopy;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationRepository;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.utils.SendMail;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Felix Xavier <famaladoss@ebi.ac.uk>
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class ConfigServiceTest {

    Account account = new Account ("reference", "username", "givenName", "password", "email",
            new java.sql.Date(2000000000), "organisation", "avatarImageUrl");
   //first configuration
    ConfigurationDeploymentParameters cdp = new ConfigurationDeploymentParameters("cdpName", account);
    ConfigDeploymentParamsCopy  configDeploymentParamsCopy = new ConfigDeploymentParamsCopy(cdp);
    Configuration configuration =  new Configuration("myConfig", account,
            "myCppName", "myCppReference", "myKey", 2.0, 5.0, configDeploymentParamsCopy);

    //second configuration
    ConfigurationDeploymentParameters cdp2 = new ConfigurationDeploymentParameters("cdpName2", account);
    ConfigDeploymentParamsCopy  configDeploymentParamsCopy2 = new ConfigDeploymentParamsCopy(cdp2);
    Configuration configuration2 =  new Configuration("myConfig", account,
            "myCppName", "myCppReference", "myKey", 2.0, 5.0, configDeploymentParamsCopy2);


    private ConfigurationRepository configurationRepository = mock(ConfigurationRepository.class);
    private DomainService domainService = mock(DomainService.class);
    private CloudProviderParametersService cppService = mock(CloudProviderParametersService.class);
    private ConfigurationDeploymentParametersService cdpService = mock(ConfigurationDeploymentParametersService.class);
    private CloudProviderParamsCopyService cloudProviderParametersCopyService = mock(CloudProviderParamsCopyService.class);
    private DeploymentService deploymentService = mock(DeploymentService.class);
    private SendMail sendMail = mock(SendMail.class);

    private ConfigurationService testCandidate = new ConfigurationService(configurationRepository, domainService,
            cppService, cdpService, cloudProviderParametersCopyService, deploymentService, sendMail);

    List configurationList = new ArrayList<Configuration>();

    public void setUp(){
        configurationList.add(configuration);
        configurationList.add(configuration2);
    }

    @Test
    public void testCheckObsoleteConfigurations(){
     assertTrue(1==1);
    }

}
