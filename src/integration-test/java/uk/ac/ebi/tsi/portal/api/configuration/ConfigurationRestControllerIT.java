package uk.ac.ebi.tsi.portal.api.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;
import org.apache.log4j.Logger;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.ac.ebi.tsc.portal.BePortalApiApplication;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.configuration.controller.ConfigurationResource;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;
import uk.ac.ebi.tsc.portal.config.WebConfiguration;
import uk.ac.ebi.tsi.portal.api.deployment.DeploymentRestControllerIT;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasSize;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {WebConfiguration.class, BePortalApiApplication.class})
@TestPropertySource("classpath:integrationTest.properties")
@AutoConfigureMockMvc
public class ConfigurationRestControllerIT {

    private static final Logger logger = Logger.getLogger(ConfigurationRestControllerIT.class);

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

    //Team owned by Karo. Ajay is a member of the team.
    private String teamNameC = "test-team3";

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

   // @Test
    public void canReturnObsoleteConfiguration() throws Exception {


        //create cloud credentials
        String cppJson = "{\"name\": \"ois6\", \"cloudProvider\": \"openstack\", \"fields\":[]}";
        String cppResponse = mockMvc.perform(
                post("/cloudproviderparameters")
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cppJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        CloudProviderParameters cpp = mapper.readValue(cppResponse, CloudProviderParameters.class);

        //create deployment parameters
        String dpJson = "{\"name\": \"ois6\", \"fields\":[]}";
        String dpResponse = mockMvc.perform(
                post("/configuration/deploymentparameters")
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dpJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        ConfigurationDeploymentParameters dp = mapper.readValue(dpResponse, ConfigurationDeploymentParameters.class);

        //create configuration
        ConfigurationResource configurationResource = new ConfigurationResource();
        configurationResource.setName("ois6");
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

        //now delete the cloud credential delete
        mockMvc.perform(
                MockMvcRequestBuilders
                        .delete("/cloudproviderparameters/{name}", cpp.getName())
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());

        //try and get the cpp
       mockMvc.perform(
                get("/cloudproviderparameters/os6" )
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isNotFound());

        //now get the current user configurations

        mockMvc.perform(
                get("/configuration")
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"obsolete\":true")));

    }

  //  @Test
    public void canReturnObsoleteSharedConfiguration() throws Exception {

        //create cloud credentials
        String cppJson = "{\"name\": \"newos6\", \"cloudProvider\": \"openstack\", \"fields\":[]}";
        String cppResponse = mockMvc.perform(
                post("/cloudproviderparameters")
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cppJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        CloudProviderParameters cpp = mapper.readValue(cppResponse, CloudProviderParameters.class);

        //create deployment parameters
        String dpJson = "{\"name\": \"newos6\", \"fields\":[]}";
        String dpResponse = mockMvc.perform(
                post("/configuration/deploymentparameters")
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dpJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        ConfigurationDeploymentParameters dp = mapper.readValue(dpResponse, ConfigurationDeploymentParameters.class);

        //create configuration
        ConfigurationResource configurationResource = new ConfigurationResource();
        configurationResource.setName("newos6");
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

        //Share the cloud credential with Team C
        mockMvc.perform(
                post("/team/"+teamNameC+"/cloudproviderparameters")
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \""+cpp.getName()+"\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Share the created configuration with Team C

        mockMvc.perform(
                post("/team/"+teamNameC+"/configuration")
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \""+configuration.getName()+"\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        //now delete the cloud credential delete
        mockMvc.perform(
                MockMvcRequestBuilders
                        .delete("/cloudproviderparameters/{name}", cpp.getName())
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());

        //try and get the cpp
        mockMvc.perform(
                get("/cloudproviderparameters/newos6" )
                        .headers(createHeaders(getToken(testUserName, testPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isNotFound());


        //now get the ajay user configurations
        mockMvc.perform(
                get("/configuration/shared")
                        .headers(createHeaders(getToken(ajayUserName, ajayPassword)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"obsolete\":true")));

    }

    protected HttpHeaders createHeaders(String token) {
        return new HttpHeaders() {
            private static final long serialVersionUID = 1L;
            {
                String authHeader = "Bearer " + token;
                set("Authorization", authHeader);
                set("Content-Type", "application/json");
                set("Accept", "application/hal+json");
            }};
    }


    private String getToken(String username, String password) {
        ResponseEntity<String> response = restTemplate.withBasicAuth(username, password)
                .getForEntity(aapUrl, String.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
        return response.getBody();
    }

}
