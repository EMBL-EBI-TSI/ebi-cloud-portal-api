package uk.ac.ebi.tsi.portal.api.deployment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ebi.tsc.portal.BePortalApiApplication;
import uk.ac.ebi.tsc.portal.api.application.controller.ApplicationResource;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.configuration.controller.ConfigurationResource;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;
import uk.ac.ebi.tsc.portal.api.deployment.controller.DeploymentResource;
import uk.ac.ebi.tsc.portal.api.deployment.repo.Deployment;
import uk.ac.ebi.tsc.portal.config.WebConfiguration;

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
	private String aapUserName;
	
	@Value("${aapPassword}")
	private String aapPassword;
	
	@Value("${aapUrl}")
	private String aapUrl;

	private String token;

	@Before
	public void setup() throws Exception{
		//get jwt token
		ResponseEntity<String> response = restTemplate.withBasicAuth(aapUserName, aapPassword)
				.getForEntity(aapUrl, String.class);
		token = response.getBody();
	}


	@Test
	public void testIfDeploymentsCanBeCreated() throws Exception{ 
		//create cloud credentials
		String cppJson = "{\"name\": \"os6\", \"cloudProvider\": \"openstack\", \"fields\":[]}";
		String cppResponse = mockMvc.perform(
				post("/cloudproviderparameters")
				.headers(createHeaders(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(cppJson)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		CloudProviderParameters cpp = mapper.readValue(cppResponse, CloudProviderParameters.class);
		
		String dpJson = "{\"name\": \"os6\", \"fields\":[]}";
		String dpResponse = mockMvc.perform(
				post("/configuration/deploymentparameters")
				.headers(createHeaders(token))
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
				.headers(createHeaders(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(configJson)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Configuration configuration =  mapper.readValue(configResponse, Configuration.class);
		
		String appJson = "{\"repoUri\": \"https://github.com/EMBL-EBI-TSI/cpa-bioexcel-cwl\"}";
		String appResponse = mockMvc.perform(
				post("/application")
				.headers(createHeaders(token))
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
				.headers(createHeaders(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(deploymentJson)
				.accept(MediaType.APPLICATION_JSON)
				)
		.andExpect(status().is2xxSuccessful());
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
