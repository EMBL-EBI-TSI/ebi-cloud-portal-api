package uk.ac.ebi.tsi.portal.api.team;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.hamcrest.MatcherAssert;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.portal.BePortalApiApplication;
import uk.ac.ebi.tsc.portal.api.team.repo.Team;
import uk.ac.ebi.tsc.portal.api.utils.SendMail;
import uk.ac.ebi.tsc.portal.config.WebConfiguration;

import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {WebConfiguration.class, BePortalApiApplication.class})
@TestPropertySource(locations = {"classpath:integrationTest.properties"})
public class TeamRestControllerIT {

	private static final Logger logger = Logger.getLogger(TeamRestControllerIT.class);

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper mapper;

	@Value("${ajayUserName}")
	private String ajayUserName;

	@Value("${ajayPassword}")
	private String ajayPassword;

	@Value("${aapUrl}")
	private String aapUrl;

	@MockBean
	private DomainService domainService;

	@MockBean
	private SendMail sendMail;

	String teamName = "teamName";
	String domainReference = "domainReference";
	String domainName = "domainName";
	Team team;

	private String getToken(String username, String password) {
		ResponseEntity<String> response = restTemplate.withBasicAuth(username, password)
				.getForEntity(aapUrl, String.class);
		MatcherAssert.assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
		return response.getBody();
	}


	public Domain getDomain() {
		Domain domain = new Domain();
		domain.setDomainReference(domainReference);
		domain.setDomainName(domainName);
		return domain;
	}

	@Test
	public void can_get_a_team() throws Exception{

		mockMvc.perform(
				MockMvcRequestBuilders.get("/team")
						.headers(createHeaders(getToken(ajayUserName, ajayPassword)))
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andReturn().getResponse();
	}

	@Test
	public void can_create_a_team() throws Exception{

		given(domainService.createDomain(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.willReturn(getDomain());

		team = new Team();
		team.setName(teamName);
		String json = mapper.writeValueAsString(team);
		mockMvc.perform(
				post("/team")
						.headers(createHeaders(getToken(ajayUserName, ajayPassword)))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("name").value(teamName))
				.andExpect(jsonPath("domainReference").value(domainReference))
				.andReturn();
	}

	@Test
	public void add_member_to_team() throws Exception{

		given(domainService.addUserToDomain(any(Domain.class), any(User.class), any(String.class))).willReturn(getDomain());
		given(domainService.getDomainByReference(any(String.class), any(String.class))).willReturn(getDomain());
		doNothing().when(sendMail).send(any(Collection.class), any(String.class), any(String.class));
		String memberEmailtoAdd = "embl.ebi.tsi@gmail.com";
		String json = "{\"name\":\"" + teamName + "\", \"domainReference\":\" + domainReference + \",\"memberAccountEmails\":[\""+ memberEmailtoAdd  + "\"]}";
		logger.info("In add member to team " + json);

		String teamResponse =
				mockMvc.perform(
						post("/team/member")
								.headers(createHeaders(getToken(ajayUserName, ajayPassword)))
								.contentType(MediaType.APPLICATION_JSON)
								.content(json)
								.accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isOk())
						.andReturn().getResponse().getContentAsString();
		assertThat(teamResponse, containsString("User  was added to team and domain teamName"));
	}

	@Test
	public void canRemoveMemberFromTeam() throws Exception{

		given(domainService.removeUserFromDomain(any(User.class), any(Domain.class), any(String.class))).willReturn(getDomain());

		Collection<User> users = new ArrayList<>();
		User user =  mock(User.class);
		given(user.getEmail()).willReturn("embl.ebi.tsi@gmail.com");
		users.add(user);

		given(domainService.getAllUsersFromDomain(any(String.class), any(String.class))).willReturn(users);
		String memberEmailtoRemove = "embl.ebi.tsi@gmail.com";
		String uri = "/team/" + teamName + "/member/" + memberEmailtoRemove;
		given(domainService.removeUserFromDomain
				(any(User.class), any(Domain.class),  any(String.class))).willReturn(getDomain());
		String teamResponse =  mockMvc.perform(
				delete(uri)
						.headers(createHeaders(getToken(ajayUserName, ajayPassword)))
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(teamResponse, containsString("User embl.ebi.tsi@gmail.com was deleted from team teamName"));

	}

	@Test
	public void can_delete_a_team() throws Exception{

		given(domainService.deleteDomain(any(Domain.class), any(String.class))).willReturn(getDomain());
		String uri = "/team/" + teamName;
		mockMvc.perform(
				delete(uri)
						.headers(createHeaders(getToken(ajayUserName, ajayPassword)))
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
