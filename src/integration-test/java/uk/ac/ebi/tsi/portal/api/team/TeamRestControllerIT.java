package uk.ac.ebi.tsi.portal.api.team;

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
		
		@Value("${aapUrl}")
		private String aapUrl;
		
		private String token;
		
		String teamName = "SOME_TEAM_NAME";
		
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

}
