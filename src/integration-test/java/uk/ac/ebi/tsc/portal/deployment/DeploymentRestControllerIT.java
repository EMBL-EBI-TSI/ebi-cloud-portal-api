package uk.ac.ebi.tsc.portal.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.tsc.portal.BePortalApiApplication;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.repo.AccountRepository;
import uk.ac.ebi.tsc.portal.api.application.controller.ApplicationResource;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopy;
import uk.ac.ebi.tsc.portal.api.configuration.controller.ConfigurationResource;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;
import uk.ac.ebi.tsc.portal.api.deployment.controller.DeploymentResource;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentApplication;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentRepository;
import uk.ac.ebi.tsc.portal.api.deployment.service.ConfigurationNotUsableForApplicationException;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDeployer;
import uk.ac.ebi.tsc.portal.config.WebConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:integrationTest.properties")
@AutoConfigureMockMvc
public class DeploymentRestControllerIT {

    private static final Logger logger = Logger.getLogger(DeploymentRestControllerIT.class);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    // User Karo on AAP explore
    private String TEST_USER_REFERENCE = "usr-b070585b-a340-4a98-aff1-f3de48da8c38";

    // User  Ajay on AAP explore
    private String AJAY_USER_REFERENCE = "usr-e8c1d6d5-6bf4-4636-a70e-41b8f32c70b4";

    //Team owned by Ajay. Karo is a member of the team.
    private String teamNameA = "test-team1";

    //Team owned by Ajay. Karo is a member of the team.
    private String teamNameB = "test-team2";

    @Value("${aapUserName}")
    private String testUserName;

    @Value("${aapPassword}")
    private String testPassword;

    @Value("${ajayUserName}")
    private String ajayUserName;

    @Value("${ajayPassword}")
    private String ajayPassword;

    @Value("${aapUrl}")
    private String aapUrl;

    @Value("${be.applications.root}")
    private String applicationRootDir;

    @MockBean
    ApplicationDeployer applicationDeployer;

    @Autowired
    DeploymentRepository deploymentRepository;

    @Autowired
    AccountRepository accountRepository;

    @MockBean
    DeploymentApplication deploymentApplication;

    @Value("${pantherUserName}")
    private String pantherUserName;

    @Value("${pantherPassword}")
    private String pantherPassword;


    @Test
    public void canCreateDeployment() throws Exception {

        doNothing().when(applicationDeployer).deploy(anyString(), Mockito.any(Application.class), anyString(),
                anyString(), anyMap(), anyMap(), anyMap(), anyMap(), Mockito.any(CloudProviderParamsCopy.class),
                Mockito.any(Configuration.class), Mockito.any(Timestamp.class), anyString(), anyString());

        //create cloud credentials
        String cppJson = "{\"name\": \"os6\", \"cloudProvider\": \"openstack\", \"fields\":[]}";
        String cppResponse = mockMvc.perform(
                post("/cloudproviderparameters")
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cppJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        CloudProviderParameters cpp = mapper.readValue(cppResponse, CloudProviderParameters.class);

        String dpJson = "{\"name\": \"os6\", \"fields\":[]}";
        String dpResponse = mockMvc.perform(
                post("/configuration/deploymentparameters")
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dpJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        ConfigurationDeploymentParameters dp = mapper.readValue(dpResponse, ConfigurationDeploymentParameters.class);

        ConfigurationResource configurationResource = new ConfigurationResource();
        configurationResource.setName("os6");
        configurationResource.setCloudProviderParametersName(cpp.getName());
        configurationResource.setDeploymentParametersName(dp.getName());
        configurationResource.setSshKey("some key");
        String configJson = mapper.writeValueAsString(configurationResource);
        String configResponse = mockMvc.perform(
                post("/configuration")
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Configuration configuration = mapper.readValue(configResponse, Configuration.class);

        String appJson = "{\"repoUri\": \"https://github.com/EMBL-EBI-TSI/cpa-bioexcel-cwl\"}";
        String appResponse = mockMvc.perform(
                post("/application")
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appJson)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        ApplicationResource app = mapper.readValue(appResponse, ApplicationResource.class);

        DeploymentResource deployment = new DeploymentResource();
        deployment.applicationName = app.getName();
        deployment.setApplicationAccountUsername(app.getAccountUsername());
        deployment.applicationName = app.getName();
        deployment.setConfigurationName(configuration.getName());
        deployment.setConfigurationAccountUsername(app.getAccountUsername());
        deployment.accountUsername = app.getAccountUsername();
        deployment.assignedInputs = new ArrayList();
        deployment.assignedParameters = new ArrayList();
        deployment.attachedVolumes = new ArrayList();
        deployment.cloudProviderParametersCopy = null;

        String deploymentResponse = mockMvc.perform(
                post("/deployment")
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(deployment))
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        DeploymentResource karoDeploymentResource = mapper.readValue(deploymentResponse, DeploymentResource.class);
        assertThat(karoDeploymentResource.accountEmail, containsString("embl.ebi.tsi@gmail.com"));
        //assertThat(deploymentRepository.findByReference(karoDeploymentResource.reference).get().getReference(), is(karoDeploymentResource.reference));
        logger.info(deploymentRepository.findAll().size() + "SDASDas");
        deploymentRepository.findAll().forEach(d -> {
            logger.info("referenceshdhss " + d.getReference());
        });
        //assertThat(deploymentRepository.findAll().size(), is(1));
    }

    @Test
    @Transactional
    /***
     *  Case : User cannot deploy own Application with non existent Configuration
     */
    public void cannotDeployAppWithNonExistentConfig() throws Exception {

        String testUserToken = getToken(testUserName, testPassword);
        String configName = "some-config";

        // Creating an application for test user
        String appJson = "{\"repoUri\": \"https://github.com/EMBL-EBI-TSI/cpa-instance\"}";
        String appResponse = mockMvc.perform(
                post("/application")
                        .headers(createHeaders(testUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appJson)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        ApplicationResource application = mapper.readValue(appResponse, ApplicationResource.class);

        // Deploying application with some-config which does not exist
        String deploymentJson = "{\"applicationName\":\"" + application.getName() + "\"," +
                "\"applicationAccountUsername\":\"" + TEST_USER_REFERENCE + "\"," +
                "\"cloudProviderParametersCopy\":null," +
                "\"attachedVolumes\":[]," +
                "\"assignedInputs\":[],\"assignedParameters\":[]," +
                "\"configurationName\":\"" + configName + "\"," +
                "\"configurationAccountUsername\":\"" + TEST_USER_REFERENCE + "\"}";

        mockMvc.perform(
                post("/deployment")
                        .headers(createHeaders(testUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deploymentJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("[0].message").value("Could not find configuration " + configName + " " +
                        "with owner reference '" + TEST_USER_REFERENCE + "'."))
                .andReturn();

    }

    @Test
    @Transactional
    /***
     *  Case : User cannot deploy own Application with shared configuration
     */
    public void cannotDeployAppWithSharedConfig() throws Exception {

        String testUserToken = getToken(testUserName, testPassword);
        String ajayUserToken = getToken(ajayUserName, ajayPassword);

        // Creating an application for test user
        String appJson = "{\"repoUri\": \"https://github.com/EMBL-EBI-TSI/cpa-instance\"}";
        String appResponse = mockMvc.perform(
                post("/application")
                        .headers(createHeaders(testUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appJson)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        ApplicationResource testUSerApplication = mapper.readValue(appResponse, ApplicationResource.class);

        //Creating cloud provider parameter for Ajay
        String cppJson = "{\"name\": \"os6\", \"cloudProvider\": \"openstack\", \"fields\":[]}";
        String cppResponse = mockMvc.perform(
                post("/cloudproviderparameters")
                        .headers(createHeaders(ajayUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cppJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        CloudProviderParameters cpp = mapper.readValue(cppResponse, CloudProviderParameters.class);

        //Creating deployment parameter for Ajay
        String dpJson = "{\"name\": \"os6\", \"fields\":[]}";
        String dpResponse = mockMvc.perform(
                post("/configuration/deploymentparameters")
                        .headers(createHeaders(ajayUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dpJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        ConfigurationDeploymentParameters configDP = mapper.readValue(dpResponse, ConfigurationDeploymentParameters.class);

        /**
         * Creating configuration from created cloud provider parameter and deployment parameter
         *  for Ajay
         */
        ConfigurationResource configurationResource = new ConfigurationResource();
        configurationResource.setName("os6");
        configurationResource.setCloudProviderParametersName(cpp.getName());
        configurationResource.setDeploymentParametersName(configDP.getName());
        configurationResource.setSshKey("some key");
        String configJson = mapper.writeValueAsString(configurationResource);
        String configResponse = mockMvc.perform(
                post("/configuration")
                        .headers(createHeaders(ajayUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Configuration ajayConfiguration = mapper.readValue(configResponse, Configuration.class);

        // Share the created configuration with Team A
        String shareConfig = "{\"name\": \"" + ajayConfiguration.getName() + "\"}";

        mockMvc.perform(
                post("/team/" + teamNameA + "/configuration")
                        .headers(createHeaders(ajayUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shareConfig)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Now Deploy application with shared configuration
        String deploymentJson = "{\"applicationName\":\"" + testUSerApplication.getName() + "\"," +
                "\"applicationAccountUsername\":\"" + TEST_USER_REFERENCE + "\"," +
                "\"cloudProviderParametersCopy\":null," +
                "\"attachedVolumes\":[]," +
                "\"assignedInputs\":[],\"assignedParameters\":[]," +
                "\"configurationName\":\"" + ajayConfiguration.getName() + "\"," +
                "\"configurationAccountUsername\":\"" + AJAY_USER_REFERENCE + "\"}";

        MvcResult result = mockMvc.perform(
                post("/deployment")
                        .headers(createHeaders(testUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deploymentJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();
        Optional<ConfigurationNotUsableForApplicationException> exception = Optional.ofNullable((ConfigurationNotUsableForApplicationException)
                result.getResolvedException());
        exception.ifPresent((e) -> assertThat(e, is(instanceOf(ConfigurationNotUsableForApplicationException.class))));
    }

    @Test
    @Transactional
    /***
     *  Case : User cannot deploy Application which is shared through Team A with Shared configuration
     *  which is shared through Team B
     */
    public void cannotDeployAppAndConfigSharedInDifferentTeam() throws Exception {

        String testUserToken = getToken(testUserName, testPassword);
        String ajayUserToken = getToken(ajayUserName, ajayPassword);

        // Creating an application for Ajay
        String appJson = "{\"repoUri\": \"https://github.com/EMBL-EBI-TSI/cpa-instance\"}";
        String appResponse = mockMvc.perform(
                post("/application")
                        .headers(createHeaders(ajayUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appJson)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        ApplicationResource ajayUSerApplication = mapper.readValue(appResponse, ApplicationResource.class);

        // Sharing the application with Team A
        String shareApplication = "{\"name\": \"" + ajayUSerApplication.getName() + "\"}";

        mockMvc.perform(
                post("/team/" + teamNameA + "/application")
                        .headers(createHeaders(getToken(ajayUserName, ajayPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shareApplication)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());


        //Creating cloud provider parameter for Ajay
        String cppJson = "{\"name\": \"os6\", \"cloudProvider\": \"openstack\", \"fields\":[]}";
        String cppResponse = mockMvc.perform(
                post("/cloudproviderparameters")
                        .headers(createHeaders(ajayUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cppJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        CloudProviderParameters cpp = mapper.readValue(cppResponse, CloudProviderParameters.class);

        //Creating deployment parameter for Ajay
        String dpJson = "{\"name\": \"os6\", \"fields\":[]}";
        String dpResponse = mockMvc.perform(
                post("/configuration/deploymentparameters")
                        .headers(createHeaders(ajayUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dpJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        ConfigurationDeploymentParameters configDP = mapper.readValue(dpResponse, ConfigurationDeploymentParameters.class);

        /**
         * Creating configuration from existing cloud provider parameter and deployment parameter
         *  for Ajay
         */
        ConfigurationResource configurationResource = new ConfigurationResource();
        configurationResource.setName("os6");
        configurationResource.setCloudProviderParametersName(cpp.getName());
        configurationResource.setDeploymentParametersName(configDP.getName());
        configurationResource.setSshKey("some key");
        String configJson = mapper.writeValueAsString(configurationResource);
        String configResponse = mockMvc.perform(
                post("/configuration")
                        .headers(createHeaders(ajayUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Configuration ajayConfiguration = mapper.readValue(configResponse, Configuration.class);

        // Share the created configuration with Team B
        String shareConfig = "{\"name\": \"" + ajayConfiguration.getName() + "\"}";

        mockMvc.perform(
                post("/team/" + teamNameB + "/configuration")
                        .headers(createHeaders(ajayUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shareConfig)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Now Deploy application which is shared in Team A with Configuration which is shared in Team B
        String deploymentJson = "{\"applicationName\":\"" + ajayUSerApplication.getName() + "\"," +
                "\"applicationAccountUsername\":\"" + AJAY_USER_REFERENCE + "\"," +
                "\"cloudProviderParametersCopy\":null," +
                "\"attachedVolumes\":[]," +
                "\"assignedInputs\":[],\"assignedParameters\":[]," +
                "\"configurationName\":\"" + ajayConfiguration.getName() + "\"," +
                "\"configurationAccountUsername\":\"" + AJAY_USER_REFERENCE + "\"}";

        MvcResult result = mockMvc.perform(
                post("/deployment")
                        .headers(createHeaders(testUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deploymentJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();
        Optional<ConfigurationNotUsableForApplicationException> exception = Optional.ofNullable((ConfigurationNotUsableForApplicationException)
                result.getResolvedException());
        exception.ifPresent((e) -> assertThat(e, is(instanceOf(ConfigurationNotUsableForApplicationException.class))));
    }

    @Test
    @Transactional
    /***
     *  Case : User cannot deploy an Application with configuration which is created from
     *  a shared cloud provider parameter.
     */
    public void cannotDeployAppWithConfigCreatedBySharedCloudProviderParameter() throws Exception {

        String testUserToken = getToken(testUserName, testPassword);
        String ajayUserToken = getToken(ajayUserName, ajayPassword);

        // Creating an application for test user
        String appJson = "{\"repoUri\": \"https://github.com/EMBL-EBI-TSI/cpa-instance\"}";
        String appResponse = mockMvc.perform(
                post("/application")
                        .headers(createHeaders(testUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appJson)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        ApplicationResource testUSerApplication = mapper.readValue(appResponse, ApplicationResource.class);

        //Creating deployment parameter for test user
        String dpJson = "{\"name\": \"os6\", \"fields\":[]}";
        String dpResponse = mockMvc.perform(
                post("/configuration/deploymentparameters")
                        .headers(createHeaders(testUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dpJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        ConfigurationDeploymentParameters configDP = mapper.readValue(dpResponse, ConfigurationDeploymentParameters.class);

        //Creating cloud provider parameter for Ajay
        String cppJson = "{\"name\": \"os6\", \"cloudProvider\": \"openstack\", \"fields\":[]}";
        String cppResponse = mockMvc.perform(
                post("/cloudproviderparameters")
                        .headers(createHeaders(ajayUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cppJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        CloudProviderParameters cpp = mapper.readValue(cppResponse, CloudProviderParameters.class);

        //Sharing cloud provider parameter with Team A
        String shareCloudProviderParamter = "{\"name\": \"" + cpp.getName() + "\"}";

        mockMvc.perform(
                post("/team/" + teamNameA + "/cloudproviderparameters")
                        .headers(createHeaders(ajayUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shareCloudProviderParamter)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        /**
         * Creating configuration from shared cloud provider parameter and deployment parameter
         *  for Test user
         */
        ConfigurationResource configurationResource = new ConfigurationResource();
        configurationResource.setName("os6");
        configurationResource.setCloudProviderParametersName(cpp.getName());
        configurationResource.setDeploymentParametersName(configDP.getName());
        configurationResource.setSshKey("some key");
        String configJson = mapper.writeValueAsString(configurationResource);
        String configResponse = mockMvc.perform(
                post("/configuration")
                        .headers(createHeaders(testUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Configuration testUserConfiguration = mapper.readValue(configResponse, Configuration.class);

        // Now Deploy application with Configuration which is created from shared cloud provider parameter
        String deploymentJson = "{\"applicationName\":\"" + testUSerApplication.getName() + "\"," +
                "\"applicationAccountUsername\":\"" + TEST_USER_REFERENCE + "\"," +
                "\"cloudProviderParametersCopy\":null," +
                "\"attachedVolumes\":[]," +
                "\"assignedInputs\":[],\"assignedParameters\":[]," +
                "\"configurationName\":\"" + testUserConfiguration.getName() + "\"," +
                "\"configurationAccountUsername\":\"" + TEST_USER_REFERENCE + "\"}";

        MvcResult result = mockMvc.perform(
                post("/deployment")
                        .headers(createHeaders(testUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deploymentJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();
        Optional<ConfigurationNotUsableForApplicationException> exception = Optional.ofNullable((ConfigurationNotUsableForApplicationException)
                result.getResolvedException());
        exception.ifPresent((e) -> assertThat(e, is(instanceOf(ConfigurationNotUsableForApplicationException.class))));

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

    @After
    public void tearDownAfterClass() throws Exception {
        // Cleaning application directories after test execution.
        File file = new File(applicationRootDir + "/" + TEST_USER_REFERENCE);
        deleteFile(file);
        file = new File(applicationRootDir + "/" + AJAY_USER_REFERENCE);
        deleteFile(file);
    }

    private void deleteFile(File file) throws IOException {
        if (file.exists())
            FileUtils.forceDelete(file);
    }

}
