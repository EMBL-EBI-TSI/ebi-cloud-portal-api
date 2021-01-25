package uk.ac.ebi.tsi.portal.api.deployment;

import org.apache.log4j.Logger;
import org.json.JSONObject;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.tsc.portal.BePortalApiApplication;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDeployer;
import uk.ac.ebi.tsc.portal.config.WebConfiguration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {WebConfiguration.class, BePortalApiApplication.class})
@TestPropertySource("classpath:integrationTest.properties")
@AutoConfigureMockMvc
public class DeploymentRestController2ViewDeploymentsIT {

	private static final Logger logger = Logger.getLogger(DeploymentRestController2ViewDeploymentsIT.class);

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private MockMvc mockMvc;

	@Value("${aapUserName}")
	private String testUserName;

	@Value("${aapPassword}")
	private String testPassword;

	@Value("${ajayUserName}")
	private String ajayUserName;

	@Value("${ajayPassword}")
	private String ajayPassword;

	@Value("${pantherUserName}")
	private String pantherUserName;

	@Value("${pantherPassword}")
	private String pantherPassword;

	@Value("${aapUrl}")
	private String aapUrl;

	@MockBean
	ApplicationDeployer applicationDeployer;

	String deployment_reference;

	@Before
	public void getAllDeployments() throws Exception {

		MockHttpServletResponse deploymentsResponse = mockMvc.perform(
				get("/deployment")
						.headers(createHeaders(getToken(testUserName, testPassword)))
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andReturn().getResponse();
		JSONObject embeddedObject = new JSONObject(deploymentsResponse.getContentAsString());
		JSONObject deploymentsObject = new JSONObject(embeddedObject.get("_embedded").toString());
		deployment_reference = deploymentsObject.getJSONArray("deploymentResourceList").getJSONObject(0).getString("reference");
	}

	@Test
	public void creatorCanViewDeployment() throws Exception {
		String uri = "/deployment/" + deployment_reference;
		MockHttpServletResponse response = mockMvc.perform(
				get(uri)
						.headers(createHeaders(getToken(testUserName, testPassword)))
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andReturn().getResponse();
	}

	@Test
	public void nonCreatorCannotViewDeployment() throws Exception {
		String uri = "/deployment/" + deployment_reference;
		MockHttpServletResponse response = mockMvc.perform(
				get(uri)
						.headers(createHeaders(getToken(ajayUserName, ajayPassword)))
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isForbidden())
				.andReturn().getResponse();
	}

	@Test
	public void nonCreatorButAdminCanViewDeployment() throws Exception {
		String uri = "/deployment/" + deployment_reference;
		MockHttpServletResponse response = mockMvc.perform(
				get(uri)
						.headers(createHeaders(getToken(pantherUserName, pantherPassword)))
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andReturn().getResponse();
	}

	/*@Test
	//@WithUserDetails(value = "ajay", userDetailsServiceBeanName = "customUserDetailsService", setupBefore = TestExecutionEvent.TEST_METHOD)
	public void nonCreatorButAdminCanViewDeployment() throws Exception {
		String uri = "/deployment/" + deployment_reference;
		MockHttpServletResponse response = mockMvc.perform(
						 get(uri)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andReturn().getResponse();
	}*/

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

	private String getToken(String username, String password) {
		ResponseEntity<String> response = restTemplate.withBasicAuth(username, password)
				.getForEntity(aapUrl, String.class);
		assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
		return response.getBody();
	}
}
