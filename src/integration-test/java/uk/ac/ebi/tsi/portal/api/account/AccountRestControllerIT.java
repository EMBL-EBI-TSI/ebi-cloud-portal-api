package uk.ac.ebi.tsi.portal.api.account;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import uk.ac.ebi.tsc.portal.BePortalApiApplication;
import uk.ac.ebi.tsc.portal.config.WebConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {WebConfiguration.class, BePortalApiApplication.class})
@TestPropertySource("classpath:integrationTest.properties")
@AutoConfigureMockMvc
public class AccountRestControllerIT {
	
    private static final Logger logger = Logger.getLogger(AccountRestControllerIT.class);

	@Autowired
    private TestRestTemplate restTemplate;
    
	@Autowired
	private MockMvc mockMvc;
	
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
	public void testLogin() throws Exception{
		MockHttpServletResponse response = mockMvc.perform(
															get("/account")
															.headers(createHeaders(token))
															.contentType(MediaType.APPLICATION_JSON)
															.accept(MediaType.APPLICATION_JSON)
														   )
		.andExpect(status().isOk())
		.andReturn().getResponse();
		
		logger.info("Response "  +response);
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
