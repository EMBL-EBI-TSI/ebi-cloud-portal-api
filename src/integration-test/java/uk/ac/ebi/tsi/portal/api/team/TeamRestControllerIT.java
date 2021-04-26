package uk.ac.ebi.tsi.portal.api.team;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.portal.BePortalApiApplication;
import uk.ac.ebi.tsc.portal.api.team.service.TeamAccessDeniedException;
import uk.ac.ebi.tsc.portal.config.WebConfiguration;
import uk.ac.ebi.tsc.portal.security.EcpAuthenticationService;

import java.util.*;

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
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private EcpAuthenticationService authenticationService;

    @Rule
    public WireMockRule mockDomainService = new WireMockRule(wireMockConfig().port(9000));


	@Before
	public void setup() {
		when(authenticationService.getAuthentication(any())).thenReturn(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	@WithMockUser(username = "usr-e8c1d6d5-6bf4-4636-a70e-41b8f32c70b4")
	public void can_get_a_team() throws Exception {
		MockHttpServletResponse response = mockMvc.perform(
				get("/team")
						.header("Authorization", "Bearer sometoken")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andReturn().getResponse();

		logger.info("Response " + response);
	}

	@Test
	@WithMockUser(username = "usr-e8c1d6d5-6bf4-4636-a70e-41b8f32c70b4")
	public void team_owner_can_add_team_contact_emails() throws Exception {

		String emails = "contact1@ebi,contact2@ebi";
		mockMvc.perform(
				put("/team/" + "test-team1" + "/contactemail/")
						.header("Authorization", "Bearer sometoken")
						.contentType(MediaType.APPLICATION_JSON)
						.content(emails)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "usr-e8c1d6d5-6bf4-4636-a70e-41b8f32c70b4")
	public void team_owner_can_remove_team_contact_emails() throws Exception {

		String deleteURL = "/team/" + "test-team1" + "/contactemail/" + "contact@ebi";
		mockMvc.perform(
				delete(deleteURL)
						.header("Authorization", "Bearer sometoken")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent());
	}

	@Test
	@WithMockUser(username = "usr-b070585b-a340-4a98-aff1-f3de48da8c38")
	public void non_team_owner_cannot_add_team_contact_emails() throws Exception {

		String emails = "contact1@ebi,contact2@ebi";
		try {
			mockMvc.perform(
					put("/team/" + "test-team1" + "/contactemail/")
							.header("Authorization", "Bearer sometoken")
							.contentType(MediaType.APPLICATION_JSON)
							.content(emails)
							.accept(MediaType.APPLICATION_JSON))
			;
		} catch (NestedServletException e) {
			assertEquals(e.getCause().getClass(), TeamAccessDeniedException.class);
		}
	}

	@Test
	@WithMockUser(username = "usr-b070585b-a340-4a98-aff1-f3de48da8c38")
	public void non_team_owner_cannot_remove_team_contact_emails() throws Exception {

		String deleteURL = "/team/" + "test-team1" + "/contactemail/" + "contact@ebi";
		try {
			mockMvc.perform(
					post(deleteURL)
							.header("Authorization", "Bearer sometoken")
							.contentType(MediaType.APPLICATION_JSON)
							.accept(MediaType.APPLICATION_JSON))
			;
		} catch (NestedServletException e) {
			assertEquals(e.getCause().getClass(), TeamAccessDeniedException.class);
		}
	}

	@Test
	@WithMockUser(username = "usr-b070585b-a340-4a98-aff1-f3de48da8c38")
	public void getAllTeamsForCurrentUser() throws Exception {
		mockMvc.perform(
				get("/team/all")
						.header("Authorization", "Bearer sometoken")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk());
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
	public void non_team_owner_cannot_see_team_manager_emails() throws Exception {

		String domainManagersUrl = "/domains/dom-e0de1881-d284-401a-935e-8979b328b158/managers";
		mockDomainService.givenThat(WireMock.get(domainManagersUrl).willReturn(aResponse().withStatus(HttpStatus.FORBIDDEN.value())));
		mockMvc.perform(
				get("/team/test-team1")
						.header("Authorization", "Bearer sometoken")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.managerEmails", hasSize(0)));;

	}

	@Test
	@WithMockUser(username = "usr-e8c1d6d5-6bf4-4636-a70e-41b8f32c70b4")
	public void getMemberTeamsForUserTeamOwner() throws Exception {

		teamOwnerManagementDomainsSetup();
		mockMvc.perform(
				get("/team/member")
						.header("Authorization", "Bearer sometoken")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.teamResourceList[0].managerEmails", hasSize(1)))
				.andExpect(jsonPath("$._embedded.teamResourceList[1].managerEmails", hasSize(1)));
	}

	@Test
	@WithMockUser(username = "usr-b070585b-a340-4a98-aff1-f3de48da8c38")
	public void getMemberTeamsForUserNotTeamOwnerorManager() throws Exception {

		nonTeamOwnerManagementDomainsSetup();
		mockMvc.perform(
				get("/team/member")
						.header("Authorization", "Bearer sometoken")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.teamResourceList[0].managerEmails", hasSize(0)))
				.andExpect(jsonPath("$._embedded.teamResourceList[1].managerEmails", hasSize(0)));
	}

	@Test
	@WithMockUser(username = "usr-e8c1d6d5-6bf4-4636-a70e-41b8f32c70b4")
	public void team_owner_can_see_all_team_member_emails() throws Exception {

		String domainManagersUrl = "/domains/dom-e0de1881-d284-401a-935e-8979b328b158/managers";
		String managersString = mapper.writeValueAsString(getManagers());
		mockDomainService.givenThat(WireMock.get(domainManagersUrl).willReturn(okJson(managersString)));
		mockMvc.perform(
				get("/team/test-team1")
						.header("Authorization", "Bearer sometoken")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberAccountEmails", hasSize(2)));
	}

	@Test
	@WithMockUser(username = "usr-b070585b-a340-4a98-aff1-f3de48da8c38")
	public void non_team_owner_cannot_see_other_team_member_emails() throws Exception {

		String domainManagersUrl = "/domains/dom-e0de1881-d284-401a-935e-8979b328b158/managers";
		mockDomainService.givenThat(WireMock.get(domainManagersUrl).willReturn(aResponse().withStatus(HttpStatus.FORBIDDEN.value())));
		mockMvc.perform(
				get("/team/test-team1")
						.header("Authorization", "Bearer sometoken")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberAccountEmails", hasSize(1)));

	}

	@Test
	@WithMockUser(username = "usr-b070585b-a340-4a98-aff1-f3de48da8c38")
	public void get_member_teams_member_emails_for_not_teamowner() throws Exception {

		nonTeamOwnerManagementDomainsSetup();
		mockMvc.perform(
				get("/team/member")
						.header("Authorization", "Bearer sometoken")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.teamResourceList[0].memberAccountEmails", hasSize(1)))
				.andExpect(jsonPath("$._embedded.teamResourceList[1].memberAccountEmails", hasSize(1)));
	}

	@Test
	@WithMockUser(username = "usr-e8c1d6d5-6bf4-4636-a70e-41b8f32c70b4")
	public void get_member_teams_member_emails_for_user_teamOwner() throws Exception {

		teamOwnerManagementDomainsSetup();
		mockMvc.perform(
				get("/team/member")
						.header("Authorization", "Bearer sometoken")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.teamResourceList[0].memberAccountEmails", hasSize(2)))
				.andExpect(jsonPath("$._embedded.teamResourceList[1].memberAccountEmails", hasSize(2)));
	}

	private void teamOwnerManagementDomainsSetup() throws JsonProcessingException {
		String domainManagementUrl = "/my/management";
		String domainCollectionString = mapper.writeValueAsString(getDomains());
		mockDomainService.givenThat(WireMock.get(domainManagementUrl).willReturn(okJson(domainCollectionString)));
		String domainManagersUrl1 = "/domains/dom-e0de1881-d284-401a-935e-8979b328b158/managers";
		String domainManagersUrl2 = "/domains/dom-4f412d31-cde5-452d-8536-b650a0b7b5d4/managers";
		String managersString = mapper.writeValueAsString(getManagers());
		mockDomainService.givenThat(WireMock.get(domainManagersUrl1).willReturn(okJson(managersString)));
		mockDomainService.givenThat(WireMock.get(domainManagersUrl2).willReturn(okJson(managersString)));
	}

	private void nonTeamOwnerManagementDomainsSetup() throws JsonProcessingException {
		String domainManagementUrl = "/my/management";
		List<Domain> domainCollection = new ArrayList<>();
		mockDomainService.givenThat(WireMock.get(domainManagementUrl).willReturn(okJson(mapper.writeValueAsString(domainCollection))));
		Set<User> managers = new HashSet<>();
		String domainManagersUrl1 = "/domains/dom-e0de1881-d284-401a-935e-8979b328b158/managers";
		String domainManagersUrl2 = "/domains/dom-4f412d31-cde5-452d-8536-b650a0b7b5d4/managers";
		String managersString = mapper.writeValueAsString(managers);
		mockDomainService.givenThat(WireMock.get(domainManagersUrl1).willReturn(aResponse().withStatus(HttpStatus.FORBIDDEN.value())));
		mockDomainService.givenThat(WireMock.get(domainManagersUrl2).willReturn(aResponse().withStatus(HttpStatus.FORBIDDEN.value())));
	}

	private Collection<Domain> getDomains() {
		List<Domain> domainCollection = new ArrayList<>();
		Domain domainOne = new Domain("domainOne", "domainOne desc", "dom-e0de1881-d284-401a-935e-8979b328b158");
		Domain domainTwo = new Domain("domainTwo", "domainTwo desc", "dom-4f412d31-cde5-452d-8536-b650a0b7b5d4");
		domainCollection.add(domainOne);
		domainCollection.add(domainTwo);
		return domainCollection;
	}

	private Collection<User> getManagers(){
		Set<User> managers = new HashSet<>();
		User managerOne = new User("userName", "email", "userReference", "fullname", null);
		managers.add(managerOne);
		return managers;
	}
}
