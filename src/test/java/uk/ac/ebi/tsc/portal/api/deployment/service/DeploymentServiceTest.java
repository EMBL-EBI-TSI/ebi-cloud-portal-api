package uk.ac.ebi.tsc.portal.api.deployment.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.deployment.repo.*;

import java.security.Principal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class DeploymentServiceTest {

    DeploymentStatusEnum[] activeStatuses = {
            DeploymentStatusEnum.STARTING,
            DeploymentStatusEnum.STARTING_FAILED,
            DeploymentStatusEnum.RUNNING,
            DeploymentStatusEnum.RUNNING_FAILED};

    @Mock
    DeploymentRepository deploymentRepository;

    @Mock
    DeploymentStatusRepository deploymentStatusRepository;

    DeploymentService deploymentService;

    Account account = new Account ("reference", "username", "givenName", "password", "email",
            new java.sql.Date(2000000000), "organisation", "avatarImageUrl");


    Application application =  new Application("repoUri", "repoPath", "name", "reference", account);
    DeploymentApplication deploymentApplication = new DeploymentApplication(application);

    //first deployment
    String reference = "reference";
    Deployment deployment =  new Deployment(reference, account, deploymentApplication,
            "cloudProviderParametersReference", "userSshKey");
    DeploymentStatus deploymentStatus = new DeploymentStatus(deployment, DeploymentStatusEnum.STARTING);

    //second deployment
    Deployment deploymentTwo =  new Deployment("reference2", account, deploymentApplication,
            "cloudProviderParametersReference", "userSshKey");
    DeploymentStatus deploymentStatusTwo = new DeploymentStatus(deployment, DeploymentStatusEnum.DESTROYING);

    String configurationReference = "configurationReference";

    @Before
    public void setUp() {
        deploymentService = new DeploymentService(deploymentRepository, deploymentStatusRepository );
    }

    @Test
    public void testFindDeploymentsShowActive(){

        //first deployment
        deployment.setDeploymentStatus(deploymentStatus);

        List<Deployment> deployments = new ArrayList<>();
        deployments.add(deployment);

       when(deploymentRepository.findByAccountUsernameAndDeploymentStatusStatusIn(account.getUsername(), Arrays.asList(activeStatuses)))
              .thenReturn(deployments);
        assertTrue(deploymentService.findDeployments(deployment.getAccount().getUsername(), true).size() ==1);
    }

    @Test
    public void testFindDeploymentsShowAll(){

        //first deployment
        deployment.setDeploymentStatus(deploymentStatus);
        //second deployment
        deploymentTwo.setDeploymentStatus(deploymentStatusTwo);

        List<Deployment> deployments = new ArrayList<>();
        deployments.add(deployment);
        deployments.add(deploymentTwo);
        when(this.deploymentRepository.findByAccountUsername(account.getUsername()))
             .thenReturn(deployments);
        assertTrue(deploymentService.findDeployments(deployment.getAccount().getUsername(), false).size() ==2);
    }

    @Test
    public void testFindDeploymentsByReferenceAndStatusShowActive(){
        //first deployment
        deployment.setDeploymentStatus(deploymentStatus);

        List<Deployment> deployments = new ArrayList<>();
        deployments.add(deployment);

        when(deploymentRepository.findByDeploymentConfigurationConfigurationReferenceAndDeploymentStatusStatusIn(configurationReference, Arrays.asList(activeStatuses)))
                .thenReturn(deployments);
        assertTrue(deploymentService.findDeploymentsByConfigurationReferenceAndDeploymentStatus(configurationReference, true).size() ==1);
    }

    @Test
    public void testFindDeploymentsByReferenceAndStatusShowAll(){

        //first deployment
        deployment.setDeploymentStatus(deploymentStatus);

        //second deployment
        deploymentTwo.setDeploymentStatus(deploymentStatusTwo);

        List<Deployment> deployments = new ArrayList<>();
        deployments.add(deployment);
        deployments.add(deploymentTwo);
        when(deploymentRepository.findByDeploymentConfigurationConfigurationReference(configurationReference))
                .thenReturn(deployments);
        assertTrue(deploymentService.findDeploymentsByConfigurationReferenceAndDeploymentStatus(configurationReference, false).size() ==2);
    }
}
