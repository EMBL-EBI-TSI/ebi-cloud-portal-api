package uk.ac.ebi.tsi.portal.api.application;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.tsc.portal.BePortalApiApplication;
import uk.ac.ebi.tsc.portal.api.application.controller.ApplicationResource;
import uk.ac.ebi.tsc.portal.config.WebConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {WebConfiguration.class, BePortalApiApplication.class})
@TestPropertySource("classpath:integrationTest.properties")
@AutoConfigureMockMvc
public class ApplicationRestControllerIT {

    private static final Logger logger = Logger.getLogger(ApplicationRestControllerIT.class);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Value("${aapUserName}")
    private String aapUserName;

    @Value("${aapPassword}")
    private String aapPassword;

    @Value("${aapUrl}")
    private String aapUrl;

    private String token;

    static ApplicationResource applicationResource;

    @Before
    public void setup() throws Exception{
        //get jwt token
        ResponseEntity<String> response = restTemplate.withBasicAuth(aapUserName, aapPassword)
                .getForEntity(aapUrl, String.class);
        token = response.getBody();
    }

    @Test
    public void canGetAllApplications() throws Exception {

        mockMvc.perform(
                get("/application")
                        .headers(createHeaders(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk());
    }

    //@Test
    public void testCreateAndDeleteApplication() throws Exception{
        String json = "{\"repoUri\": \"https://github.com/EMBL-EBI-TSI/cpa-tesk\"}";
        logger.info("Application repo uri " + json);
        String appResponse = mockMvc.perform(
                post("/application")
                        .headers(createHeaders(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        applicationResource = mapper.readValue(appResponse, ApplicationResource.class);
        mockMvc.perform(
                delete("/application/{name}/" , applicationResource.getName())
                        .headers(createHeaders(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk());
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

}
