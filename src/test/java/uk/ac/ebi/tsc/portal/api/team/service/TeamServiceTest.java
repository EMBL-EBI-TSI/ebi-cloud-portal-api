package uk.ac.ebi.tsc.portal.api.team.service;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.util.InMemoryResource;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.TokenService;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.service.AccountService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyService;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentConfigurationService;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;
import uk.ac.ebi.tsc.portal.api.team.repo.TeamRepository;
import uk.ac.ebi.tsc.portal.api.utils.SendMail;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDeployer;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TeamServiceTest {

    String defaultTeamsFile = "ecp.default.teams.file";
    String ecpAapUsername = "ecpAapUsername";
    String ecpAapPassword = "ecpAapPassword";
    private TeamService teamService;
    private AccountService accountService = mock(AccountService.class);
    private DomainService domainService = mock(DomainService.class);
    private Account account = mock(Account.class);
    private Domain domain = mock(Domain.class);
    private DeploymentConfigurationService depConfigService = mock(DeploymentConfigurationService.class);
    private DeploymentService deploymentService = mock(DeploymentService.class);
    private CloudProviderParamsCopyService cppCopyService = mock(CloudProviderParamsCopyService.class);
    private SendMail sendMail = mock(SendMail.class);
    private ResourceLoader resourceLoader = mock(ResourceLoader.class);
    private TeamRepository teamRepository = mock(TeamRepository.class);
    private ApplicationDeployer applicationDeployer = mock(ApplicationDeployer.class);
    private TokenService tokenService = mock(TokenService.class);

    public TeamServiceTest() {
        when(resourceLoader.getResource(defaultTeamsFile)).thenReturn(new InMemoryResource("[\n" +
                "  {\n" +
                "    \"emailDomain\":\"test.com\",\n" +
                "    \"teamName\": \"TEST1\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"emailDomain\":\"test.org\",\n" +
                "    \"teamName\": \"TEST2\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"emailDomain\":\"test.org\",\n" +
                "    \"teamName\": \"TEST3\"\n" +
                "  }\n" +
                "]"));
    }

    @Before
    public void setUp() throws IOException {
        teamService = new TeamService(teamRepository, accountService, domainService,
                deploymentService, cppCopyService, depConfigService, applicationDeployer, sendMail, tokenService,
                resourceLoader, ecpAapUsername, ecpAapPassword, defaultTeamsFile);
    }

    @Test
    public void testAddToDefaultTeam() {
        given(account.getUsername()).willReturn("username");
        given(account.getEmail()).willReturn("anEmail@test.com");
        String teamName = "TEST1";
        Team team = new Team();
        team.setName(teamName);
        String domainReference = "domainReference";
        team.setDomainReference(domainReference);
        Domain domain = mock(Domain.class);
        when(domainService.getDomainByReference(anyString(), anyString())).thenReturn(domain);
        when(domainService.addUserToDomain(any(), any(), any())).thenReturn(domain);
        when(tokenService.getAAPToken(ecpAapUsername, ecpAapPassword)).thenReturn(anyString());
        when(teamRepository.findByName(teamName)).thenReturn(Optional.of(team));
        when(teamRepository.findTeamByName(teamName)).thenReturn(Optional.of(team));
        teamService.addAccountToDefaultTeamsByEmail(account);
        assertTrue(team.accountsBelongingToTeam.size() == 1);
    }

}
