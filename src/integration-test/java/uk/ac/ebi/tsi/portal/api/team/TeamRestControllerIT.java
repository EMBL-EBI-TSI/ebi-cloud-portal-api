package uk.ac.ebi.tsi.portal.api.team;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.portal.BePortalApiApplication;
import uk.ac.ebi.tsc.portal.api.team.service.TeamAccessDeniedException;
import uk.ac.ebi.tsc.portal.config.WebConfiguration;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {WebConfiguration.class, BePortalApiApplication.class})
@TestPropertySource("classpath:integrationTest.properties")
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

	@Value("${ajayUserName}")
	private String ajayUserName;

	@Value("${ajayPassword}")
	private String ajayPassword;

	@MockBean
	private DomainService domainService;

	@MockBean
	private Domain domain;
		
		@Before
		public void setup() throws Exception{
			//get jwt token
			ResponseEntity<String> response = restTemplate.withBasicAuth(aapUserName, aapPassword)
					.getForEntity(aapUrl, String.class);
			token = response.getBody();
		}

		@Test
		public void can_get_a_team() throws Exception{
			MockHttpServletResponse response = mockMvc.perform(
					get("/team")
					.headers(createHeaders(token))
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					)
					.andExpect(status().isOk())
					.andReturn().getResponse();

			logger.info("Response "  +response);
		}

	@Test
	public void team_owner_can_add_team_contact_emails() throws Exception {

		String emails = "contact1@ebi,contact2@ebi";
		mockMvc.perform(
				put("/team/" + "test-team1" + "/contactemail/")
						.headers(createHeaders(getToken(ajayUserName, ajayPassword)))
						.contentType(MediaType.APPLICATION_JSON)
						.content(emails)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

	}

	@Test
	public void team_owner_can_remove_team_contact_emails() throws Exception {

		String deleteURL = "/team/" + "test-team1" + "/contactemail/" + "contact@ebi";
		mockMvc.perform(
				delete(deleteURL)
						.headers(createHeaders(getToken(ajayUserName, ajayPassword)))
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent());
	}

	@Test
	public void non_team_owner_cannot_add_team_contact_emails() throws Exception {

		String emails = "contact1@ebi,contact2@ebi";
		try {
			mockMvc.perform(
					put("/team/" + "test-team1" + "/contactemail/")
							.headers(createHeaders(token))
							.contentType(MediaType.APPLICATION_JSON)
							.content(emails)
							.accept(MediaType.APPLICATION_JSON))
			;
		} catch (NestedServletException e) {
			assertEquals(e.getCause().getClass(), TeamAccessDeniedException.class);
		}
	}

	@Test
	public void non_team_owner_cannot_remove_team_contact_emails() throws Exception {

		String deleteURL = "/team/" + "test-team1" + "/contactemail/" + "contact@ebi";
		try {
			mockMvc.perform(
					post(deleteURL)
							.headers(createHeaders(token))
							.contentType(MediaType.APPLICATION_JSON)
							.accept(MediaType.APPLICATION_JSON))
			;
		} catch (NestedServletException e) {
			assertEquals(e.getCause().getClass(), TeamAccessDeniedException.class);
		}
	}

	@Test
	public void getAllTeamsForCurrentUser() throws Exception {
		mockMvc.perform(
				get("/team/all")
						.headers(createHeaders(token))
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk());
	}

	private String getToken(String username, String password) {
		ResponseEntity<String> response = restTemplate.withBasicAuth(username, password)
				.getForEntity(aapUrl, String.class);
		return response.getBody();
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
