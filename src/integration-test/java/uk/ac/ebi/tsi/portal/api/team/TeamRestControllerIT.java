package uk.ac.ebi.tsi.portal.api.team;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.NestedServletException;
import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.portal.BePortalApiApplication;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;
import uk.ac.ebi.tsc.portal.config.WebConfiguration;
import uk.ac.ebi.tsc.portal.security.EcpAuthenticationService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {WebConfiguration.class, BePortalApiApplication.class})
@TestPropertySource(value = "classpath:integrationTest.properties", properties = {"aap.domains.url = http://localhost:9000"})
@AutoConfigureMockMvc
public class TeamRestControllerIT {

    private static final Logger logger = Logger.getLogger(TeamRestControllerIT.class);

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

    String teamName = "SOME_TEAM_NAME";

    @MockBean
    private EcpAuthenticationService authenticationService;

    @Rule
    public WireMockRule mockDomainService = new WireMockRule(wireMockConfig().port(9000));

    @Before
    public void setup() throws Exception {
        //get jwt token
        ResponseEntity<String> response = restTemplate.withBasicAuth(aapUserName, aapPassword)
                .getForEntity(aapUrl, String.class);
        token = response.getBody();
        when(authenticationService.getAuthentication(any())).thenReturn(SecurityContextHolder.getContext().getAuthentication());
    }


    public void can_get_a_team() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(
                get("/team")
                        .headers(createHeaders(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andReturn().getResponse();

        logger.info("Response " + response);
    }


    public void can_create_a_team() throws Exception {
        Team team = new Team();
        team.setName(teamName);
        String json = mapper.writeValueAsString(team);
        mockMvc.perform(
                post("/team")
                        .headers(createHeaders(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("name").value(teamName))
                .andReturn();
    }


    @Test
    @WithMockUser(username = "usr-e8c1d6d5-6bf4-4636-a70e-41b8f32c70b4")
    public void team_owner_can_see_team_manager_emails() throws Exception {

        String domainManagersUrl = "/domains/dom-e0de1881-d284-401a-935e-8979b328b158/managers";
        String managersString = mapper.writeValueAsString(getManagers());
        mockDomainService.givenThat(WireMock.get(domainManagersUrl).willReturn(okJson(managersString)));
        mockMvc.perform(
                get("/team/test-team1")
						.header("Authorization", "Bearer sometoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managerEmails", hasSize(1)));
    }

    @Test
    @WithMockUser(username = "usr-b070585b-a340-4a98-aff1-f3de48da8c38")
    public void non_team_owner_can_see_team_manager_emails() throws Exception {

        String domainManagersUrl = "/domains/dom-e0de1881-d284-401a-935e-8979b328b158/managers";
        mockDomainService.givenThat(WireMock.get(domainManagersUrl).willReturn(aResponse().withStatus(HttpStatus.FORBIDDEN.value())));
        try{
            mockMvc.perform(
                    get("/team/test-team1")
                            .header("Authorization", "Bearer sometoken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andReturn();
        }catch (NestedServletException e) {
            assertEquals(e.getCause().getClass(), HttpClientErrorException.class);
        }
    }

    private String getToken(String username, String password) {
        ResponseEntity<String> response = restTemplate.withBasicAuth(username, password)
                .getForEntity(aapUrl, String.class);
        return response.getBody();
    }

    private Collection<User> getManagers(){
		Set<User> managers = new HashSet<>();
		User managerOne = new User("userName", "email", "userReference", "fullname", null);
		managers.add(managerOne);
		return managers;
	}

		/*@Test
		public void add_member_to_team() throws Exception{
			
			String json = "{\"name\":\"" + teamName + "\", \"memberAccountEmails\":[\""+ email + "\"]}";
			logger.info("In add member to team " + json);
			mockMvc.perform(
					post("/team/member") 
					.headers(createHeaders(token))
					.contentType(MediaType.APPLICATION_JSON)
					.content(json)
					.accept(MediaType.APPLICATION_JSON))

			.andExpect(status().isOk());

		}
		

		@Test
		public void canRemoveMemberFromTeam() throws Exception{

			String uri = "/team/" + teamName + "/member/" + email;
			mockMvc.perform(
					delete(uri) 
					.headers(createHeaders(token))
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		}*/


    public void can_delete_a_team() throws Exception {
        mockMvc.perform(
                delete("/team/", teamName)
                        .headers(createHeaders(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
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
            }
        };
    }

}
