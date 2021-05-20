package uk.ac.ebi.tsi.portal.api.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.repo.TokenService;
import uk.ac.ebi.tsc.portal.BePortalApiApplication;
import uk.ac.ebi.tsc.portal.api.team.service.TeamService;
import uk.ac.ebi.tsc.portal.config.WebConfiguration;
import uk.ac.ebi.tsc.portal.security.EcpAuthenticationService;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
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

    // https://github.com/tomakehurst/wiremock/issues/485 - hanging wiremock
    @ClassRule
    public static WireMockRule mockDomainService = new WireMockRule(wireMockConfig().port(9000));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private EcpAuthenticationService authenticationService;

    @Autowired
    TeamService teamService;

    @MockBean
    private TokenService tokenService;

    @Value("${aapUserName}")
    private String testUserName;

    @Value("${aapPassword}")
    private String testPassword;

    @Before
    public void setup() {
        when(authenticationService.getAuthentication(any())).thenReturn(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @WithMockUser(username = "usr-eeaa9825-44e6-46ad-9da6-26fcfdba6f8b")
    public void canGetAllApplications() throws Exception {

        when(tokenService.getAAPToken(testUserName, testPassword)).thenReturn("atoken");
        Domain domainOne = new Domain("domainOne", "domainOne desc", "dom-e0de1991-d284-401a-935e-8979b328b765");
        mockDomainService.givenThat(WireMock.get("/domains/dom-e0de1991-d284-401a-935e-8979b328b765").willReturn(okJson(mapper.writeValueAsString(domainOne))));
        mockDomainService.givenThat(WireMock.put("/domains/dom-e0de1991-d284-401a-935e-8979b328b765/usr-eeaa9825-44e6-46ad-9da6-26fcfdba6f8b/user").willReturn(okJson(mapper.writeValueAsString(domainOne))));
        mockMvc.perform(
                get("/application").header("Authorization", "Bearer sometoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk());
        assertEquals(teamService.findByNameAndGetAccounts("EMBL-EBI-IT").getAccountsBelongingToTeam().size(),1);
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
