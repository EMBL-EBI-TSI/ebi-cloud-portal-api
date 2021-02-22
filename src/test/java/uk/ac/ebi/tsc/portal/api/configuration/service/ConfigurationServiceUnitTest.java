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
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopy;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParametersNotFoundException;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParametersService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyNotFoundException;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyService;
import uk.ac.ebi.tsc.portal.api.configuration.controller.ConfigurationResource;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigDeploymentParamsCopy;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationRepository;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;
import uk.ac.ebi.tsc.portal.api.utils.SendMail;

import java.util.*;

import static org.junit.Assert.assertTrue;
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
    String configurationReference1 = "configurationReference1";
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
    String configurationReference2 = "configurationReference2";
    Configuration configuration2;

    //two teams
    Team team1, team2;
    String team1Name = "teamName1";
    String team2Name = "teamName2";
    String domainReference1 = "domainReference1";
    String domainReference2 = "domainReference2";
    Account memberAccount = new Account("memreference", "memusername", "memgivenName", "mempassword", "memmail",
            new java.sql.Date(2000000000), "memorganisation", "memavatarImageUrl");
    Set<Team> memberTeams;

    //Application
    Application application = new Application( "repoUri", "repoPath", "appname", "appreference", account);

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

        //memberAccount is member of team1
        team1 = new Team();
        team1.setName(team1Name);
        team1.setDomainReference(domainReference1);

        team1.setAccount(account);
        team2 = new Team();
        team2.setName(team2Name);
        team2.setDomainReference(domainReference2);
        team2.setAccount(account);

        memberTeams = new HashSet<>();
        memberTeams.add(team1);
        memberTeams.add(team2);
        memberAccount.setMemberOfTeams(memberTeams);


        //first configuration, shared with 2 teams
        cdp1 = new ConfigurationDeploymentParameters(cdp1Name, account);
        configDeploymentParamsCopy1 = new ConfigDeploymentParamsCopy(cdp1);
        cppCopy1 = new  CloudProviderParamsCopy(cpp1Name, cloudProvider, account);
        cpp1 = new CloudProviderParameters(cpp1Name, cloudProvider, account);
        configuration1 = new Configuration("myConfig", account,
                cppCopy1.getName(), cppReference1, "myKey", 2.0, 5.0, configDeploymentParamsCopy1);
        configuration1.setReference(configurationReference1);

        //second configuration, shared with 2 teams
        cdp2 = new ConfigurationDeploymentParameters(cdp2Name, account);
        configDeploymentParamsCopy2 = new ConfigDeploymentParamsCopy(cdp2);
        cppCopy2 = new  CloudProviderParamsCopy(cpp2Name, cloudProvider, account);
        cpp2 = new CloudProviderParameters(cpp2Name, cloudProvider, account);
        configuration2 = new Configuration("myConfig", account,
                cppCopy2.getName(), cppReference2, "myKey", 2.0, 5.0, configDeploymentParamsCopy2);
        configuration2.setReference(configurationReference2);


        cpp1.setReference(cppReference1);
        cpp2.setReference(cppReference2);
        cppCopy1.setCloudProviderParametersReference(cppReference1);
        cppCopy2.setCloudProviderParametersReference(cppReference2);
        cdp1.setReference(cdpReference1);
        cpp2.setReference(cppReference2);
        configDeploymentParamsCopy1.setConfigurationDeploymentParametersReference(cdpReference1);
        configDeploymentParamsCopy2.setConfigurationDeploymentParametersReference(cdpReference2);

        // Controller needs RequestContextHolder and HttpServlet Request
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @Test
    public void testCheckObsoleteConfigurationsNoneOwnedConfiguration() {

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
        given(cdpCopyService.findByConfigurationDeploymentParametersReference(cdpReference1)).willReturn(configDeploymentParamsCopy1);
        given(cdpCopyService.findByConfigurationDeploymentParametersReference(cdpReference2)).willReturn(configDeploymentParamsCopy2);
        given(cdpService.findByReference(configDeploymentParamsCopy1.getConfigurationDeploymentParametersReference())).willReturn(cdp1);
        given(cdpService.findByReference(configDeploymentParamsCopy2.getConfigurationDeploymentParametersReference())).willReturn(cdp2);
        List<ConfigurationResource> obsoleteConfigurationList = testCandidate.checkObsoleteConfigurations(configResourceList,
                account, cdpCopyService);

        assertTrue(obsoleteConfigurationList .get(0).isObsolete() == false);
        assertTrue(obsoleteConfigurationList .get(1).isObsolete() == false);

    }

    @Test
    public void testCheckObsoleteConfigurationsOwnedConfigurationCPPNotFound() {


        List configurationList = new ArrayList<Configuration>();
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
        given(cppService.findByReference(cppReference1)).willThrow(CloudProviderParametersNotFoundException.class);
        given(cppService.findByReference(cppReference2)).willReturn(cpp2);
        given(cdpCopyService.findByConfigurationDeploymentParametersReference(cdpReference1)).willReturn(configDeploymentParamsCopy1);
        given(cdpCopyService.findByConfigurationDeploymentParametersReference(cdpReference2)).willReturn(configDeploymentParamsCopy2);
        given(cdpService.findByReference(configDeploymentParamsCopy1.getConfigurationDeploymentParametersReference())).willReturn(cdp1);
        given(cdpService.findByReference(configDeploymentParamsCopy2.getConfigurationDeploymentParametersReference())).willReturn(cdp2);

        List<ConfigurationResource> obsoleteConfigurationList = testCandidate.checkObsoleteConfigurations(configResourceList,
                account, cdpCopyService);

        assertTrue(obsoleteConfigurationList .get(0).isObsolete() == true);
        assertTrue(obsoleteConfigurationList .get(1).isObsolete() == false);

    }

    @Test
    public void testCheckObsoleteConfigurationsOwnedConfigurationCDPNotFound() {

        List configurationList = new ArrayList<Configuration>();
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
        given(cdpCopyService.findByConfigurationDeploymentParametersReference(cdpReference1)).willReturn(configDeploymentParamsCopy1);
        given(cdpCopyService.findByConfigurationDeploymentParametersReference(cdpReference2)).willReturn(configDeploymentParamsCopy2);
        given(cdpService.findByReference(configDeploymentParamsCopy1.getConfigurationDeploymentParametersReference())).willThrow(ConfigurationDeploymentParametersNotFoundException.class);
        given(cdpService.findByReference(configDeploymentParamsCopy2.getConfigurationDeploymentParametersReference())).willReturn(cdp2);

        List<ConfigurationResource> obsoleteConfigurationList = testCandidate.checkObsoleteConfigurations(configResourceList,
                account, cdpCopyService);

        assertTrue(obsoleteConfigurationList .get(0).isObsolete() == true);
        assertTrue(obsoleteConfigurationList .get(1).isObsolete() == false);

    }

   // @Test
    public void testCheckObsoleteConfigurationsNoneSharedConfigurationCPPNotShared() {

        Set<Team> configurationSharedWithTeams =  new HashSet<>();
        configurationSharedWithTeams.add(team1);
        configuration1.setSharedWithTeams(configurationSharedWithTeams);

        List configurationList = new ArrayList<Configuration>();
        configurationList.add(configuration1);
        configurationList.add(configuration2);

        ConfigurationResource configurationResource1 = new ConfigurationResource(configuration1, cppCopy1);
        ConfigurationResource configurationResource2 = new ConfigurationResource(configuration2, cppCopy2);
        configurationResource1.setConfigDeploymentParametersReference(cdpReference1);
        configurationResource2.setConfigDeploymentParametersReference(cdpReference2);
        configurationResource1.setReference(configurationReference1);
        configurationResource2.setReference(configurationReference2);

        configResourceList.add(configurationResource1);
        configResourceList.add(configurationResource2);

        given(cppCopyService.findByCloudProviderParametersReference(cppReference1)).willReturn(cppCopy1);
        given(cppCopyService.findByCloudProviderParametersReference(cppReference2)).willReturn(cppCopy2);
        given(cppService.findByReference(cppReference1)).willReturn(cpp1);
        given(cppService.findByReference(cppReference2)).willReturn(cpp2);
        given(cdpCopyService.findByConfigurationDeploymentParametersReference(cdpReference1)).willReturn(configDeploymentParamsCopy1);
        given(cdpCopyService.findByConfigurationDeploymentParametersReference(cdpReference2)).willReturn(configDeploymentParamsCopy2);
        given(cdpService.findByReference(configDeploymentParamsCopy1.getConfigurationDeploymentParametersReference())).willReturn(cdp1);
        given(cdpService.findByReference(configDeploymentParamsCopy2.getConfigurationDeploymentParametersReference())).willReturn(cdp2);

        given(configurationRepository.findByReference(configurationReference1)).willReturn(Optional.of(configuration1));
        given(configurationRepository.findByReference(configurationReference2)).willReturn(Optional.of(configuration2));
        List<ConfigurationResource> obsoleteConfigurationList = testCandidate.checkObsoleteConfigurations(configResourceList,
                memberAccount, cdpCopyService);

        assertTrue(obsoleteConfigurationList .get(0).isObsolete() == true);
        assertTrue(obsoleteConfigurationList .get(1).isObsolete() == true);

    }

    //@Test
    public void testCheckObsoleteConfigurationsNoneSharedConfigurationCPPSharedWithTeam1() {

        Set<Team> cppSharedTeams = new HashSet<>();
        cppSharedTeams.add(team1);
        cpp1.setSharedWithTeams(cppSharedTeams);

        Set<Team> cdpSharedTeams = new HashSet<>();
        cdpSharedTeams.add(team1);
        cdp1.setSharedWithTeams(cdpSharedTeams);

        Set<Team> configurationSharedWithTeams =  new HashSet<>();
        configurationSharedWithTeams.add(team1);
        configuration1.setSharedWithTeams(configurationSharedWithTeams);

        List configurationList = new ArrayList<Configuration>();
        configurationList.add(configuration1);
        configurationList.add(configuration2);

        ConfigurationResource configurationResource1 = new ConfigurationResource(configuration1, cppCopy1);
        ConfigurationResource configurationResource2 = new ConfigurationResource(configuration2, cppCopy2);
        configurationResource1.setConfigDeploymentParametersReference(cdpReference1);
        configurationResource2.setConfigDeploymentParametersReference(cdpReference2);
        configurationResource1.setReference(configurationReference1);
        configurationResource2.setReference(configurationReference2);

        configResourceList.add(configurationResource1);
        configResourceList.add(configurationResource2);

        given(cppCopyService.findByCloudProviderParametersReference(cppReference1)).willReturn(cppCopy1);
        given(cppCopyService.findByCloudProviderParametersReference(cppReference2)).willReturn(cppCopy2);
        given(cppService.findByReference(cppReference1)).willReturn(cpp1);
        given(cppService.findByReference(cppReference2)).willReturn(cpp2);
        given(cdpCopyService.findByConfigurationDeploymentParametersReference(cdpReference1)).willReturn(configDeploymentParamsCopy1);
        given(cdpCopyService.findByConfigurationDeploymentParametersReference(cdpReference2)).willReturn(configDeploymentParamsCopy2);
        given(cdpService.findByReference(configDeploymentParamsCopy1.getConfigurationDeploymentParametersReference())).willReturn(cdp1);
        given(cdpService.findByReference(configDeploymentParamsCopy2.getConfigurationDeploymentParametersReference())).willReturn(cdp2);

        given(configurationRepository.findByReference(configurationReference1)).willReturn(Optional.of(configuration1));
        given(configurationRepository.findByReference(configurationReference2)).willReturn(Optional.of(configuration2));
        List<ConfigurationResource> obsoleteConfigurationList = testCandidate.checkObsoleteConfigurations(configResourceList,
                memberAccount, cdpCopyService);

        assertTrue(obsoleteConfigurationList .get(0).isObsolete() == false);
        assertTrue(obsoleteConfigurationList .get(1).isObsolete() == true);

    }

   // @Test
    public void testCheckObsoleteConfigurationsNoneSharedConfigurationCPPAndCDPSharedWithTeam1() {

        Set<Team> cppSharedTeams = new HashSet<>();
        cppSharedTeams.add(team1);
        cpp1.setSharedWithTeams(cppSharedTeams);

        Set<Team> cdpSharedTeams = new HashSet<>();
        cdpSharedTeams.add(team1);
        cdp1.setSharedWithTeams(cdpSharedTeams);

        Set<Team> configurationSharedWithTeams =  new HashSet<>();
        configurationSharedWithTeams.add(team1);
        configuration1.setSharedWithTeams(configurationSharedWithTeams);

        List configurationList = new ArrayList<Configuration>();
        configurationList.add(configuration1);
        configurationList.add(configuration2);

        ConfigurationResource configurationResource1 = new ConfigurationResource(configuration1, cppCopy1);
        ConfigurationResource configurationResource2 = new ConfigurationResource(configuration2, cppCopy2);
        configurationResource1.setConfigDeploymentParametersReference(cdpReference1);
        configurationResource2.setConfigDeploymentParametersReference(cdpReference2);
        configurationResource1.setReference(configurationReference1);
        configurationResource2.setReference(configurationReference2);

        configResourceList.add(configurationResource1);
        configResourceList.add(configurationResource2);

        given(cppCopyService.findByCloudProviderParametersReference(cppReference1)).willReturn(cppCopy1);
        given(cppCopyService.findByCloudProviderParametersReference(cppReference2)).willReturn(cppCopy2);
        given(cppService.findByReference(cppReference1)).willReturn(cpp1);
        given(cppService.findByReference(cppReference2)).willReturn(cpp2);
        given(cdpCopyService.findByConfigurationDeploymentParametersReference(cdpReference1)).willReturn(configDeploymentParamsCopy1);
        given(cdpCopyService.findByConfigurationDeploymentParametersReference(cdpReference2)).willReturn(configDeploymentParamsCopy2);
        given(cdpService.findByReference(configDeploymentParamsCopy1.getConfigurationDeploymentParametersReference())).willReturn(cdp1);
        given(cdpService.findByReference(configDeploymentParamsCopy2.getConfigurationDeploymentParametersReference())).willReturn(cdp2);

        given(configurationRepository.findByReference(configurationReference1)).willReturn(Optional.of(configuration1));
        given(configurationRepository.findByReference(configurationReference2)).willReturn(Optional.of(configuration2));
        List<ConfigurationResource> obsoleteConfigurationList = testCandidate.checkObsoleteConfigurations(configResourceList,
                memberAccount, cdpCopyService);

        assertTrue(obsoleteConfigurationList .get(0).isObsolete() == false);
        assertTrue(obsoleteConfigurationList .get(1).isObsolete() == true);

    }

    @Test
    public void testCanConfigurationBeUsedForApplicationOwnConfigurationAndApplicationValidCPP(){

        given(cppService.findByReference(cppReference1)).willReturn(cpp1);
        given(cppService.canCredentialBeUsedForApplication(cpp1, application, account)).willCallRealMethod();
        boolean canConfigurationBeUsedForApplication = testCandidate.canConfigurationBeUsedForApplication(configuration1, application, account);
        assertTrue(canConfigurationBeUsedForApplication == true);

    }

    @Test
    public void testCanConfigurationBeUsedForApplicationOwnConfigurationAndApplicationInValidCPP(){

        given(cppService.findByReference(cppReference1)).willThrow(CloudProviderParametersNotFoundException.class);
        boolean canConfigurationBeUsedForApplication = testCandidate.canConfigurationBeUsedForApplication(configuration1, application, account);
        assertTrue(canConfigurationBeUsedForApplication == false);

    }

    @Test
    public void testCanConfigurationBeUsedForApplicationSharedConfigurationAndApplicationValidCPP(){

        Set<Team> cppSharedTeams = new HashSet<>();
        cppSharedTeams.add(team1);
        cpp1.setSharedWithTeams(cppSharedTeams);

        Set<Team> appSharedTeams = new HashSet<>();
        appSharedTeams.add(team1);
        application.setSharedWithTeams(appSharedTeams);

        Set<Team> configurationSharedWithTeams =  new HashSet<>();
        configurationSharedWithTeams.add(team1);
        configuration1.setSharedWithTeams(configurationSharedWithTeams);

        given(cppService.findByReference(cppReference1)).willReturn(cpp1);
        given(cppService.checkForOverlapingAmongTeams(Mockito.anySet(),Mockito.anySet(), Mockito.anySet())).willCallRealMethod();
        boolean canConfigurationBeUsedForApplication = testCandidate.canConfigurationBeUsedForApplication(configuration1, application, memberAccount);
        assertTrue(canConfigurationBeUsedForApplication == true);

    }

    @Test
    public void testCanConfigurationBeUsedForApplicationSharedConfigurationAndApplicationDifferentTeams(){

        Set<Team> cppSharedTeams = new HashSet<>();
        cppSharedTeams.add(team1);
        cpp1.setSharedWithTeams(cppSharedTeams);

        Set<Team> appSharedTeams = new HashSet<>();
        appSharedTeams.add(team2);
        application.setSharedWithTeams(appSharedTeams);

        Set<Team> configurationSharedWithTeams =  new HashSet<>();
        configurationSharedWithTeams.add(team1);
        configuration1.setSharedWithTeams(configurationSharedWithTeams);

        given(cppService.findByReference(cppReference1)).willReturn(cpp1);
        given(cppService.checkForOverlapingAmongTeams(Mockito.anySet(),Mockito.anySet(), Mockito.anySet())).willCallRealMethod();
        boolean canConfigurationBeUsedForApplication = testCandidate.canConfigurationBeUsedForApplication(configuration1, application, memberAccount);
        assertTrue(canConfigurationBeUsedForApplication == false);

    }

    @Test
    public void testCreateConfigurationResource(){

        String cppCopyNotFoundName = "cppCopyNotFoundName";
        String cppCopyNotFoundReference = "cppCopyNotFoundReference";

        ConfigurationDeploymentParameters cdp3 = new ConfigurationDeploymentParameters("cdp3Name", account);
        ConfigDeploymentParamsCopy notFoundCopy = new ConfigDeploymentParamsCopy(cdp3);
        List configurationList = new ArrayList<Configuration>();
        configurationList.add(configuration1);
        configurationList.add(configuration2);


        Configuration oneWithoutCPPCopy = new Configuration("myConfig", account,
                cppCopyNotFoundName, cppCopyNotFoundReference, "myKey", 2.0, 5.0, notFoundCopy);


        configurationList.add(oneWithoutCPPCopy);

        given(cppCopyService.findByCloudProviderParametersReference(cppReference1)).willReturn(cppCopy1);
        given(cppCopyService.findByCloudProviderParametersReference(cppReference2)).willReturn(cppCopy2);
        given(cppCopyService.findByCloudProviderParametersReference(cppCopyNotFoundReference)).willThrow(CloudProviderParamsCopyNotFoundException.class);

        List<ConfigurationResource> resourceList = testCandidate.createConfigurationResource(configurationList);
        assertTrue(resourceList.size() == 3);

    }

    @Test
    public void testCanConfigurationBeUsedForApplicationCCPAAPSharedInSameTeamButNotConfiguration(){

        Set<Team> cppSharedTeams = new HashSet<>();
        cppSharedTeams.add(team1);
        cpp1.setSharedWithTeams(cppSharedTeams);
        team1.getCppBelongingToTeam().add(cpp1);

        Set<Team> appSharedTeams = new HashSet<>();
        appSharedTeams.add(team1);
        application.setSharedWithTeams(appSharedTeams);
        team1.getApplicationsBelongingToTeam().add(application);


        Set<Team> configurationSharedWithTeams =  new HashSet<>();
        configurationSharedWithTeams.add(team2);
        configuration1.setSharedWithTeams(configurationSharedWithTeams);
        team2.getConfigurationsBelongingToTeam().add(configuration1);

        given(cppService.isCloudProviderParametersSharedWithAccount(memberAccount,cpp1)).willCallRealMethod();
        given(cppService.findByReference(cppReference1)).willReturn(cpp1);
        given(cppService.canCredentialBeUsedForApplication(cpp1, application, memberAccount)).willCallRealMethod();
        given(cppService.checkForOverlapingAmongTeams(Mockito.anySet(),Mockito.anySet(), Mockito.anySet())).willCallRealMethod();
        boolean canConfigurationBeUsedForApplication = testCandidate.canConfigurationBeUsedForApplication(configuration1, application, memberAccount);
        assertTrue(canConfigurationBeUsedForApplication == false);

    }

    @Test
    public void testCanConfigurationBeUsedForApplicationCCPAAPConfigurationSharedInSameTeam(){

        Set<Team> cppSharedTeams = new HashSet<>();
        cppSharedTeams.add(team1);
        cpp1.setSharedWithTeams(cppSharedTeams);
        team1.getCppBelongingToTeam().add(cpp1);

        Set<Team> appSharedTeams = new HashSet<>();
        appSharedTeams.add(team1);
        application.setSharedWithTeams(appSharedTeams);
        team1.getApplicationsBelongingToTeam().add(application);


        Set<Team> configurationSharedWithTeams =  new HashSet<>();
        configurationSharedWithTeams.add(team1);
        configuration1.setSharedWithTeams(configurationSharedWithTeams);
        team1.getConfigurationsBelongingToTeam().add(configuration1);

        given(cppService.isCloudProviderParametersSharedWithAccount(memberAccount,cpp1)).willCallRealMethod();
        given(cppService.findByReference(cppReference1)).willReturn(cpp1);
        given(cppService.canCredentialBeUsedForApplication(cpp1, application, memberAccount)).willCallRealMethod();
        given(cppService.checkForOverlapingAmongTeams(Mockito.anySet(),Mockito.anySet(), Mockito.anySet())).willCallRealMethod();
        boolean canConfigurationBeUsedForApplication = testCandidate.canConfigurationBeUsedForApplication(configuration1, application, memberAccount);
        assertTrue(canConfigurationBeUsedForApplication == true);

    }


}

