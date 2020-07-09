package uk.ac.ebi.tsi.portal.api.team;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Date;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.tsc.portal.BePortalApiApplication;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.service.AccountService;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;
import uk.ac.ebi.tsc.portal.config.WebConfiguration;

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

		@Value("${ajayUserName}")
		private String ajayUserName;

		@Value("${ajayPassword}")
		private String ajayPassword;
		
		@Value("${aapUrl}")
		private String aapUrl;
		
		private String token;
		
		String teamName = "some-interesting-team-name";
		String memberEmail = "ajay@email.uk";
		
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
		public void can_create_a_team() throws Exception{
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

	//	@Test
	    public void add_member_to_team() throws Exception{

		String json = "{\"name\":\"" + teamName + "\", \"memberAccountEmails\":[\""+ memberEmail + "\"]}";
		logger.info("In add member to team " + json);
		mockMvc.perform(
				post("/team/member")
						.headers(createHeaders(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.accept(MediaType.APPLICATION_JSON))

				.andExpect(status().isOk());

	}


//	@Test
	public void can_get_member_teams() throws Exception {
		//now get ajay's teams
		MockHttpServletResponse response = mockMvc.perform(
				get("/team/member")
						.headers(createHeaders(getToken(ajayUserName, ajayPassword)))
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andReturn().getResponse();

		logger.info("Response " + response.getContentAsString());

	}

		//@Test
		public void canRemoveMemberFromTeam() throws Exception{

			String uri = "/team/" + teamName + "/member/" + memberEmail;
			mockMvc.perform(
					delete(uri) 
					.headers(createHeaders(token))
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		}

		//@Test
		public void can_delete_a_team() throws Exception{
			mockMvc.perform(
					delete("/team/" , teamName)
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
			}};
		}

	private String getToken(String username, String password) {
		ResponseEntity<String> response = restTemplate.withBasicAuth(username, password)
				.getForEntity(aapUrl, String.class);
		assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
		return response.getBody();
	}

}
