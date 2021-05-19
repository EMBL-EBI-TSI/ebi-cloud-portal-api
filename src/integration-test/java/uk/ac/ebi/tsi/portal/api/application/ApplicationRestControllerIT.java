package uk.ac.ebi.tsi.portal.api.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.tsc.portal.BePortalApiApplication;
import uk.ac.ebi.tsc.portal.api.team.service.TeamService;
import uk.ac.ebi.tsc.portal.config.WebConfiguration;
import uk.ac.ebi.tsc.portal.security.EcpAuthenticationService;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {WebConfiguration.class, BePortalApiApplication.class})
@TestPropertySource(value = "classpath:integrationTest.properties", properties = {"aap.domains.url = http://localhost:9000"})
@AutoConfigureMockMvc
public class ApplicationRestControllerIT {

    private static final Logger logger = Logger.getLogger(ApplicationRestControllerIT.class);

    @Rule
    public WireMockRule mockDomainService = new WireMockRule(wireMockConfig().port(9000));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private EcpAuthenticationService authenticationService;

    @Autowired
    TeamService teamService;

    @Before
    public void setup() {
        when(authenticationService.getAuthentication(any())).thenReturn(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @WithMockUser(username = "usr-e8c1d6d5-6bf4-4636-a70e-41b8f32c70b4")
    public void canGetAllApplications() throws Exception {

        mockMvc.perform(
                get("/application").header("Authorization", "Bearer sometoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk());

        assertEquals(teamService.findByName("EMBL-EBI-IT").getAccountsBelongingToTeam().size(),1);

    }
		
		/*@Test
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
		}*/


}
