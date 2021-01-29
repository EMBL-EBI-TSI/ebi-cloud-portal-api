package uk.ac.ebi.tsc.portal.deployment;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.tsc.portal.BePortalApiApplication;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.repo.AccountRepository;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParametersField;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParametersRepository;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigDeploymentParamsCopy;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationRepository;
import uk.ac.ebi.tsc.portal.api.deployment.repo.Deployment;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentApplication;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentRepository;
import uk.ac.ebi.tsc.portal.config.WebConfiguration;

import java.util.LinkedList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {WebConfiguration.class, BePortalApiApplication.class})
@TestPropertySource("classpath:integrationTest.properties")
@AutoConfigureMockMvc
public class DeploymentRestControllerViewDeploymentsIT {

    private static final Logger logger = Logger.getLogger(DeploymentRestControllerViewDeploymentsIT.class);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Value("${aapUserName}")
    private String testUserName;

    @Value("${aapPassword}")
    private String testPassword;

    @Value("${ajayUserName}")
    private String ajayUserName;

    @Value("${ajayPassword}")
    private String ajayPassword;

    @Value("${pantherUserName}")
    private String pantherUserName;

    @Value("${pantherPassword}")
    private String pantherPassword;

    @Value("${aapUrl}")
    private String aapUrl;

    @Autowired
    DeploymentRepository deploymentRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    CloudProviderParametersRepository cloudProviderParametersRepository;

    @Autowired
    ConfigurationRepository configurationRepository;

    @Before
    public void setUp() {
        Account account = accountRepository.findOne(10001L);
        deploymentRepository.deleteAll();
        cloudProviderParametersRepository.deleteAll();
        configurationRepository.deleteAll();
        Application application = new Application("repoUri", "repoPath", "app-name", "app-reference", account);
        CloudProviderParameters cloudProviderParameters = new CloudProviderParameters("cpp-name", "cloudProvider", account);
        cloudProviderParameters.setReference("cpp-reference");
        cloudProviderParameters = cloudProviderParametersRepository.save(cloudProviderParameters);
        ConfigurationDeploymentParameters configurationDeploymentParameters = new ConfigurationDeploymentParameters("dp-name", account);
        ConfigDeploymentParamsCopy configDeploymentParamsCopy = new ConfigDeploymentParamsCopy(configurationDeploymentParameters);
        Configuration configuration = new Configuration("conf-name", account, cloudProviderParameters.getName(), cloudProviderParameters.getReference(),
                        "sshKey", null, null, configDeploymentParamsCopy);
        configuration.setReference("config-reference");
        configuration.setConfigDeployParamsReference("config-dp-reference");
        configuration = configurationRepository.save(configuration);
        DeploymentApplication deploymentApplication = new DeploymentApplication(application);
        Deployment deployment = new Deployment("TSI776767779",
                account, deploymentApplication, cloudProviderParameters.getReference(),
                configuration.getReference());
        deploymentRepository.save(deployment);
    }

    @Test
    public void creatorCanViewDeployment() throws Exception {
        String uri = "/deployment/" + "TSI776767779";
        MockHttpServletResponse response = mockMvc.perform(
                get(uri)
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    @Test
    public void creatorCanViewDeploymentStatus() throws Exception {
        String uri = "/deployment/" + "TSI776767779/status";
        MockHttpServletResponse response = mockMvc.perform(
                get(uri)
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    @Test
    public void creatorCanViewDeploymentOutputs() throws Exception {
        String uri = "/deployment/" + "TSI776767779/outputs";
        MockHttpServletResponse response = mockMvc.perform(
                get(uri)
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }


    @Test
    public void nonCreatorCannotViewDeployment() throws Exception {
        String uri = "/deployment/" + "TSI776767779";
        MockHttpServletResponse response = mockMvc.perform(
                get(uri)
                        .headers(createHeaders(getToken(ajayUserName, ajayPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isForbidden())
                .andReturn().getResponse();
    }

    @Test
    public void nonCreatorCannotViewDeploymentStatus() throws Exception {
        String uri = "/deployment/" + "TSI776767779/status";
        MockHttpServletResponse response = mockMvc.perform(
                get(uri)
                        .headers(createHeaders(getToken(ajayUserName, ajayPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isForbidden())
                .andReturn().getResponse();
    }

    @Test
    public void nonCreatorCannotViewDeploymentOutputs() throws Exception {
        String uri = "/deployment/" + "TSI776767779/outputs";
        MockHttpServletResponse response = mockMvc.perform(
                get(uri)
                        .headers(createHeaders(getToken(ajayUserName, ajayPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isForbidden())
                .andReturn().getResponse();
    }

    @Test
    public void nonCreatorButAdminCanViewDeployment() throws Exception {
        String uri = "/deployment/" + "TSI776767779";
        MockHttpServletResponse response = mockMvc.perform(
                get(uri)
                        .headers(createHeaders(getToken(pantherUserName, pantherPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    @Test
    public void nonCreatorButAdminCanViewDeploymentStatus() throws Exception {
        String uri = "/deployment/" + "TSI776767779/status";
        MockHttpServletResponse response = mockMvc.perform(
                get(uri)
                        .headers(createHeaders(getToken(pantherUserName, pantherPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    @Test
    public void nonCreatorButAdminCanViewDeploymentOutputs() throws Exception {
        String uri = "/deployment/" + "TSI776767779/outputs";
        MockHttpServletResponse response = mockMvc.perform(
                get(uri)
                        .headers(createHeaders(getToken(pantherUserName, pantherPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    protected HttpHeaders createHeaders(String token) {
        return new HttpHeaders() {
            private static final long serialVersionUID = 1L;

            {
                String authHeader = "Bearer " + token;
                set("Authorization", authHeader);
                set("Content-Type", "application/json");
                set("Accept", "application/hal+json");
            }
        };
    }

    private String getToken(String username, String password) {
        ResponseEntity<String> response = restTemplate.withBasicAuth(username, password)
                .getForEntity(aapUrl, String.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
        return response.getBody();
    }
}
