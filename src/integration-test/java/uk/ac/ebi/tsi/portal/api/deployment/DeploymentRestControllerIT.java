package uk.ac.ebi.tsi.portal.api.deployment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.apache.log4j.Logger;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.tsc.portal.BePortalApiApplication;
import uk.ac.ebi.tsc.portal.api.application.controller.ApplicationResource;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.configuration.controller.ConfigurationResource;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;
import uk.ac.ebi.tsc.portal.api.deployment.controller.DeploymentResource;
import uk.ac.ebi.tsc.portal.api.deployment.service.ConfigurationNotUsableForApplicationException;
import uk.ac.ebi.tsc.portal.config.WebConfiguration;

import java.util.ArrayList;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {WebConfiguration.class, BePortalApiApplication.class})
@TestPropertySource("classpath:integrationTest.properties")
@AutoConfigureMockMvc
public class DeploymentRestControllerIT {

	private static final Logger logger = Logger.getLogger(DeploymentRestControllerIT.class);

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private MockMvc mockMvc;

	@Autowired 
	private ObjectMapper mapper;
	
	@Value("${aapUserName}") 
	private String testUserName;
	
	@Value("${aapPassword}")
	private String testPassword;

	private String TEST_USER_REFERENCE = "usr-d8749acf-6a22-4438-accc-cc8d1877ba36";

	private String AJAY_USER_REFERENCE = "usr-9832620d-ec53-43a1-873d-efdc50d34ad1";

	@Value("${ajayUserName}")
	private String ajayUserName;

	@Value("${ajayPassword}")
	private String ajayPassword;
	
	@Value("${aapUrl}")
	private String aapUrl;


	@Test
	public void canCreateDeployment() throws Exception{


		//create cloud credentials
		String cppJson = "{\"name\": \"os6\", \"cloudProvider\": \"openstack\", \"fields\":[]}";
		String cppResponse = mockMvc.perform(
				post("/cloudproviderparameters")
				.headers(createHeaders(getToken(testUserName, testPassword)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(cppJson)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CloudProviderParameters cpp = mapper.readValue(cppResponse, CloudProviderParameters.class);
		
		String dpJson = "{\"name\": \"os6\", \"fields\":[]}";
		String dpResponse = mockMvc.perform(
				post("/configuration/deploymentparameters")
				.headers(createHeaders(getToken(testUserName, testPassword)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(dpJson)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().is2xxSuccessful())
				.andReturn().getResponse().getContentAsString();
		ConfigurationDeploymentParameters dp = mapper.readValue(dpResponse, ConfigurationDeploymentParameters.class);
		
		ConfigurationResource configurationResource = new ConfigurationResource();
		configurationResource.setName("os6");
		configurationResource.setCloudProviderParametersName(cpp.getName());
		configurationResource.setDeploymentParametersName(dp.getName());
		configurationResource.setSshKey("some key");
		String configJson = mapper.writeValueAsString(configurationResource);
		String configResponse = mockMvc.perform(
				post("/configuration")
				.headers(createHeaders(getToken(testUserName, testPassword)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(configJson)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Configuration configuration =  mapper.readValue(configResponse, Configuration.class);
		
		String appJson = "{\"repoUri\": \"https://github.com/EMBL-EBI-TSI/cpa-bioexcel-cwl\"}";
		String appResponse = mockMvc.perform(
				post("/application")
				.headers(createHeaders(getToken(testUserName, testPassword)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(appJson)
				.accept(MediaType.APPLICATION_JSON)
				)
		.andExpect(status().is2xxSuccessful())
		.andReturn().getResponse().getContentAsString();
		ApplicationResource app = mapper.readValue(appResponse, ApplicationResource.class);
		
		DeploymentResource deployment = new DeploymentResource();
		deployment.applicationName = app.getName();
		deployment.setApplicationAccountUsername(app.getAccountUsername());
		deployment.applicationName = app.getName();
		deployment.setConfigurationName(configuration.getName());
		deployment.setConfigurationAccountUsername(app.getAccountUsername());
		deployment.accountUsername = app.getAccountUsername();
		deployment.assignedInputs = new ArrayList();
		deployment.assignedParameters = new ArrayList();
		deployment.attachedVolumes =  new ArrayList();
		deployment.cloudProviderParametersCopy = null;
		
		
		String deploymentJson = mapper.writeValueAsString(deployment);
		logger.info("deploymentJson " + deploymentJson);
		
		mockMvc.perform(
				post("/deployment")
				.headers(createHeaders(getToken(testUserName, testPassword)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(deploymentJson)
				.accept(MediaType.APPLICATION_JSON)
				)
		.andExpect(status().is2xxSuccessful());
	}


	@Test
	public void cannotDeployOwnAppWithInvalidConfig() throws Exception{

		String token = getToken(testUserName, testPassword);
		String deploymentJson = "{\"applicationName\":\"Generic server instance\"," +
				"\"applicationAccountUsername\":\""+TEST_USER_REFERENCE+"\"," +
				"\"cloudProviderParametersCopy\":null," +
				"\"attachedVolumes\":[]," +
				"\"assignedInputs\":[],\"assignedParameters\":[]," +
				"\"configurationName\":\"config1\"," +
				"\"configurationAccountUsername\":\""+TEST_USER_REFERENCE+"\"}";

		mockMvc.perform(
				post("/deployment")
						.headers(createHeaders(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(deploymentJson)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("[0].message").value("Could not find configuration config1 with owner reference '"+TEST_USER_REFERENCE+"'."))
				.andReturn();

	}

	@Test
	public void cannotDeployOwnAppWithSharedConfig() throws Exception{

		String token = getToken(testUserName, testPassword);
		String deploymentJson = "{\"applicationName\":\"Generic server instance\"," +
				"\"applicationAccountUsername\":\""+TEST_USER_REFERENCE+"\"," +
				"\"cloudProviderParametersCopy\":null," +
				"\"attachedVolumes\":[]," +
				"\"assignedInputs\":[],\"assignedParameters\":[]," +
				"\"configurationName\":\"config1\"," +
				"\"configurationAccountUsername\":\""+AJAY_USER_REFERENCE+"\"}";

		MvcResult result = mockMvc.perform(
				post("/deployment")
						.headers(createHeaders(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(deploymentJson)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden())
				.andReturn();

		Optional<ConfigurationNotUsableForApplicationException> exception = Optional.ofNullable((ConfigurationNotUsableForApplicationException) result.getResolvedException());
		exception.ifPresent( (e) -> assertThat(e, is(instanceOf(ConfigurationNotUsableForApplicationException.class))));
	}

	@Test
	public void cannotDeployAppWithConfigInDifferentTeam() throws Exception{

		String token = getToken(testUserName, testPassword);
		String deploymentJson = "{\"applicationName\":\"redis\"," +
				"\"applicationAccountUsername\":\""+AJAY_USER_REFERENCE+"\"," +
				"\"cloudProviderParametersCopy\":null," +
				"\"attachedVolumes\":[]," +
				"\"assignedInputs\":[],\"assignedParameters\":[]," +
				"\"configurationName\":\"config1\"," +
				"\"configurationAccountUsername\":\""+AJAY_USER_REFERENCE+"\"}";

		MvcResult result = mockMvc.perform(
				post("/deployment")
						.headers(createHeaders(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(deploymentJson)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden())
				.andReturn();

		Optional<ConfigurationNotUsableForApplicationException> exception = Optional.ofNullable((ConfigurationNotUsableForApplicationException) result.getResolvedException());
		exception.ifPresent( (e) -> assertThat(e, is(instanceOf(ConfigurationNotUsableForApplicationException.class))));
	}

	private String getToken(String username, String password) {
		ResponseEntity<String> response = restTemplate.withBasicAuth(username, password)
				.getForEntity(aapUrl, String.class);
		assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
		return response.getBody();
	}


	@Test
	public void cannotDeployOwnAppWithOwnConfigOfSharedCC() throws Exception{

		String token = getToken(testUserName, testPassword);
		String cloudProviderParametersNameShared = "ostack provider";

		//add deployment parameters
		String deploymentParametersJson = "{\n\"name\":\"deploy_params\","
				+ "\n\"fields\":[{\n\"key\":\"floatingip_pool\",\n\"value\":\"net-external\"\n}," +
				"\n{\n\"key\":\"machine_type\",\n\"value\":\"s1.small\"\n}\n]\n}";
		mockMvc.perform(
				post("/configuration/deploymentparameters")
						.headers(createHeaders(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(deploymentParametersJson)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("name").value("deploy_params"))
				.andReturn();

		//add configuration with shared cloud provider parameter
		String configurationJson = "{\n\"name\":\"config2\",\n\"cloudProviderParametersName\":\""+cloudProviderParametersNameShared+"\"," +
				"\n\"deploymentParametersName\":\"deploy_params\"," +
				"\"sshKey\":\"\"}";
		mockMvc.perform(
				post("/configuration")
						.headers(createHeaders(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(configurationJson)
						.accept(MediaType.APPLICATION_JSON))
				.andReturn();


		String deploymentJson = "{\"applicationName\":\"Generic server instance\"," +
				"\"applicationAccountUsername\":\""+TEST_USER_REFERENCE+"\"," +
				"\"cloudProviderParametersCopy\":null," +
				"\"attachedVolumes\":[]," +
				"\"assignedInputs\":[],\"assignedParameters\":[]," +
				"\"configurationName\":\"config2\"," +
				"\"configurationAccountUsername\":\""+TEST_USER_REFERENCE+"\"}";

		MvcResult result = mockMvc.perform(
				post("/deployment")
						.headers(createHeaders(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(deploymentJson)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden())
				.andReturn();

		Optional<ConfigurationNotUsableForApplicationException> exception = Optional.ofNullable((ConfigurationNotUsableForApplicationException) result.getResolvedException());
		exception.ifPresent( (e) -> assertThat(e, is(instanceOf(ConfigurationNotUsableForApplicationException.class))));

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