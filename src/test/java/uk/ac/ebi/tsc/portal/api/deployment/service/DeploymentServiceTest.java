package uk.ac.ebi.tsc.portal.api.deployment.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.deployment.repo.Deployment;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentRepository;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentStatusRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class DeploymentServiceTest {

    @Mock
    DeploymentRepository deploymentRepository;

    @Mock
    DeploymentStatusRepository deploymentStatusRepository;

    DeploymentService deploymentService;

    @Before
    public void setUp() {
        deploymentService = new DeploymentService(deploymentRepository, deploymentStatusRepository );
    }

    @Test
    public void activeDeployments(){
        Deployment deployment = mock(Deployment.class);
        Account account = mock(Account.class);
        String username = "someusername";
        when(account.getUsername()).thenReturn(username);
        when(deployment.getAccount()).thenReturn(account);
        List<Deployment> deployments = new ArrayList<>();
        deployments.add(deployment);
        when(deploymentRepository.findByAccountUsernameAndDeploymentStatusStatusIn(Matchers.anyString(), Matchers.anyList()))
                .thenReturn(deployments);
        assertTrue(deploymentService.findDeployments(deployment.getAccount().getUsername(), true).size() ==1);
    }

    @Test
    public void allDeployments(){
        Deployment deployment = mock(Deployment.class);
        Account account = mock(Account.class);
        String username = "someusername";
        when(account.getUsername()).thenReturn(username);
        when(deployment.getAccount()).thenReturn(account);
        List<Deployment> deployments = new ArrayList<>();
        deployments.add(deployment);
        when(deploymentRepository.findByAccountUsername(Matchers.anyString()))
                .thenReturn(deployments);
        assertTrue(deploymentService.findDeployments(deployment.getAccount().getUsername(), false).size() ==1);
    }

    private Deployment deployment(String reference) {

        Deployment mockDeployment = mock(Deployment.class);
        return mockDeployment;

    }
}
