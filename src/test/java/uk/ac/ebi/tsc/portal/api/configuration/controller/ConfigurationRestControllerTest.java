package uk.ac.ebi.tsc.portal.api.configuration.controller;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.service.AccountService;
import uk.ac.ebi.tsc.portal.api.application.service.ApplicationService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParameters;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopy;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParametersService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyService;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigDeploymentParamsCopy;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationDeploymentParameters;
import uk.ac.ebi.tsc.portal.api.configuration.repo.ConfigurationRepository;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigDeploymentParamsCopyService;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationDeploymentParametersNotFoundException;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationDeploymentParametersService;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationNotFoundException;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationService;


/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class ConfigurationRestControllerTest {

	@MockBean
	private ConfigurationRestController subject;

	@MockBean
	private ConfigurationService configurationService;

	@MockBean
	private AccountService accountService;

	@MockBean
	private ApplicationService applicationService;

	@MockBean
	private ConfigurationDeploymentParametersService cdpsService;

	@MockBean
	private CloudProviderParametersService cppService;
	@MockBean
	private CloudProviderParameters cpp;

	@MockBean
	private Principal principal;

	@MockBean
	private Account account;

	@MockBean
	private HttpServletRequest request;

	@MockBean
	private uk.ac.ebi.tsc.aap.client.security.TokenHandler tokenHandler;

	@MockBean 
	private DomainService domainService;

	@MockBean
	private ConfigDeploymentParamsCopyService cdpsCopyService;

	private String principalName = "principalName";
	String tokenArray = "some token";
	String token = "token" ;

	@MockBean
	private CloudProviderParamsCopyService cppCopyService;
	
	@MockBean
	private CloudProviderParamsCopy cppCopy;

	private String cloudProviderType = "OSTACK";

	@Before
	public void setUp(){

		ReflectionTestUtils.setField(subject, "configurationService", configurationService);
		ReflectionTestUtils.setField(subject, "accountService", accountService);
		ReflectionTestUtils.setField(subject, "applicationService", applicationService);
		ReflectionTestUtils.setField(subject, "configurationDeploymentParametersService", cdpsService);
		ReflectionTestUtils.setField(cdpsService, "domainService", domainService);
		ReflectionTestUtils.setField(subject, "cloudProviderParametersService", cppService);
		ReflectionTestUtils.setField(subject, "configDeploymentParamsCopyService", cdpsCopyService);
		ReflectionTestUtils.setField(subject, "tokenHandler", tokenHandler);
		ReflectionTestUtils.setField(subject, "cloudProviderParametersCopyService", cppCopyService);
		ReflectionTestUtils.setField(configurationService, "cloudProviderParametersCopyService", cppCopyService);
		given(cppCopy.getCloudProvider()).willReturn(cloudProviderType);
		given(cppCopyService.findByCloudProviderParametersReference(Mockito.anyString())).willReturn(cppCopy);
	}

	/**
	 * test if user can get back his set configurations
	 */
	@Test
	public void testGetCurrentUserConfigurations(){

		getPrincipal();
		getAccount();
		Configuration configuration = mock(Configuration.class);
		ConfigurationResource configurationResource = mock(ConfigurationResource.class);
		List<ConfigurationResource> configurationResources = new ArrayList<>();
		configurationResources.add(configurationResource);
		List<Configuration> configurations = new ArrayList<>();
		configurations.add(configuration);
		given(configurationService.checkObsoleteConfigurations(configurationResources, account, cdpsCopyService)).willReturn(configurationResources);
		given(subject.getCurrentUserConfigurations(null, principal)).willCallRealMethod();
		Resources<ConfigurationResource> cdpsResourcesReturned = subject.getCurrentUserConfigurations(null, principal);
		assertNotNull(cdpsResourcesReturned);
	}	

	/**
	 * test if user can get all the deployment parameters
	 */
	@Test
	public void testGetAllDeploymentParameters(){

		getPrincipal();
		getAccount();
		List<ConfigurationDeploymentParameters> cdps = getDeploymentParameters();
		given(cdpsService.findByAccountUsername(principalName)).willReturn(cdps);
		given(subject.getAllDeploymentParameters(principal)).willCallRealMethod();
		Resources<ConfigurationDeploymentParametersResource> cdpsResource = subject.getAllDeploymentParameters(principal);
		assertNotNull(cdpsResource);
		assertEquals(cdpsResource.getContent().size(), 1);
	}

	/**
	 * test if user can get  deploymentparameters by its name
	 */
	@Test
	public void testGetDeploymentParametersByName(){

		getPrincipal();
		getAccount();
		ConfigurationDeploymentParameters cdp = getDeploymentParameters().get(0);
		given(cdpsService.findByNameAndAccountUserName(cdp.getName(), principalName)).willReturn(cdp);
		given(subject.getDeploymentParametersByName(principal, cdp.getName())).willCallRealMethod();
		Resource<ConfigurationDeploymentParametersResource> cdpResource = subject.getDeploymentParametersByName(principal, cdp.getName());
		assertNotNull(cdpResource);
		assertEquals(cdpResource.getContent().getName(), cdp.getName());
	}

	/**
	 * test that when configuration to be added was not added 
	 * because it was not saved,'NullPointerException' is thrown
	 */
	@Test(expected = NullPointerException.class)
	public void addConfigurationNotAdded(){

		getPrincipal();
		getAccount();
		ConfigurationResource input = getConfigurationResource();
		given(cppService.findByNameAndAccountUsername(
				input.getCloudProviderParametersName(), principal.getName())).willReturn(cpp);
		ConfigurationDeploymentParameters cdp = getDeploymentParameters().get(0);
		given(cdpsService.findByName(input.getDeploymentParametersName())).willReturn(cdp);
		given(subject.add(principal, input)).willCallRealMethod();
		ResponseEntity<?> response = subject.add(principal, input);
	}

	/**
	 * test that, when configuration to be added was not added 
	 * because it was not saved,  http status 'created' is returned
	 */
	@Test
	public void addConfigurationSuccessFullyAdded(){

		getPrincipal();
		getAccount();
		ConfigurationResource input = getConfigurationResource();
		given(cppService.findByNameAndAccountUsername(
				input.getCloudProviderParametersName(), principal.getName())).willReturn(cpp);
		ConfigurationDeploymentParameters cdp = getDeploymentParameters().get(0);
		given(cdpsService.findByName(input.getDeploymentParametersName())).willReturn(cdp);
		ResponseEntity response =  mock(ResponseEntity.class);
		given(response.getStatusCode()).willReturn(HttpStatus.CREATED);
		given(subject.add(principal, input)).willReturn(response);
		ResponseEntity<?> responseReturned = subject.add(principal, input);
		assertThat(responseReturned.getStatusCode() , is(HttpStatus.CREATED));
	}

	@Test(expected = InvalidConfigurationInputException.class)
	public void addConfigurationInvalidName(){

		String configName = "a\\.\\.\\-b0";
		getPrincipal();
		getAccount();
		String cdpsName = "cdps";
		ConfigurationDeploymentParameters cdps = new ConfigurationDeploymentParameters(cdpsName, account);
		ConfigDeploymentParamsCopy cdpsCopy = new ConfigDeploymentParamsCopy(cdps);
		String cppName = "cppName";
		String sshKey = "sshKey";
		String cppReference = "cppReference";
		List<Configuration> configurations = new ArrayList<>();
		Configuration config = new Configuration(configName, account, cppName, cppReference, sshKey, null, null, cdpsCopy );
		ConfigurationResource configResource = new ConfigurationResource(config, cppCopy);
		given(subject.add(principal, configResource)).willCallRealMethod();
		ResponseEntity<?> responseReturned = subject.add(principal, configResource);
	}


	/**
	 * test that when configuration to be added has no
	 * 'cloudprovider' which was specified 'CloudProviderParametersNotFoundException'
	 * is thrown
	 */
	@Test(expected = CloudProviderParametersNotFoundException.class)
	public void addConfigurationNoCloudProvider(){

		getPrincipal();
		getAccount();
		ConfigurationResource input = getConfigurationResource();
		given(cppService.findByNameAndAccountUsername(
				input.getCloudProviderParametersName(), principal.getName())).willThrow(CloudProviderParametersNotFoundException.class);
		given(subject.add(principal, input)).willCallRealMethod();
		subject.add(principal, input);
	}

	/**
	 * that that delete operation has necessary information
	 * supplied in method - response has no body but 
	 * it just has headers , so check response is not null
	 * 
	 */

	/*@Test
	public void testDeleteConfiguration() throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException{

		getPrincipal();
		getAccount();
		String configurationName = "configName";
		given(subject.delete(principal, configurationName)).willCallRealMethod();
		assertNotNull(subject.delete(principal, configurationName));
	}*/

	/**
	 * that that delete operation has necessary information
	 * supplied in method - response has no body but 
	 * it just has headers , so check response is not null
	 * @throws Exception 
	 * 
	 * */

	@Test
	public void testDeleteDeploymentParameters() throws Exception{

		getPrincipal();
		getAccount();
		String deploymentParametersName = "dpName";
		given(cdpsService.findByNameAndAccountUserName(deploymentParametersName, principalName)).willReturn(getDeploymentParameters().get(0));
		given(cdpsCopyService.findByNameAndAccountUsername(deploymentParametersName, principalName)).willReturn(getDeploymentParametersCopy().get(0));
		given(subject.deleteDeploymentParameters(principal, deploymentParametersName)).willCallRealMethod();
		ResponseEntity<?>  deleted = subject.deleteDeploymentParameters(principal, deploymentParametersName);
		assertNotNull(deleted);
		assertTrue(deleted.getStatusCode().equals(HttpStatus.OK));
	}

	/**
	 * test that when configuration to be added has name
	 * which has 0 characters, InvalidConfigurationInputException is thrown
	 */
	@Test(expected = InvalidConfigurationInputException.class)
	public void addConfigurationBlankConfigurationName(){

		getPrincipal();
		getAccount();
		ConfigurationResource input = getInvalidConfigurationResource();
		given(cppService.findByNameAndAccountUsername(
				input.getCloudProviderParametersName(), principal.getName())).willReturn(cpp);
		given(subject.add(principal, input)).willCallRealMethod();
		subject.add(principal, input);
	}

	/**
	 * test that when deploymentparameters to be added was not added 
	 * because it was not saved, 'NullPointerException' is thrown
	 */
	@Test(expected = NullPointerException.class)
	public void addDeploymentParametersNotAdded(){

		getPrincipal();
		getAccount();
		ConfigurationDeploymentParametersResource input = getDeploymentParametersResource();
		given(subject.addDeploymentParameters(principal, input)).willCallRealMethod();
		ResponseEntity<?> response = subject.addDeploymentParameters(principal, input);
	}

	/**
	 * test that when deploymentparameters to be added was successfully added 
	 * http status 'created' is returned
	 * 
	 */

	@Test
	public void addDeploymentParametersSuccessFullyAdded(){

		getPrincipal();
		getAccount();
		ConfigurationDeploymentParametersResource input = getDeploymentParametersResource();
		ResponseEntity response =  mock(ResponseEntity.class);
		given(response.getStatusCode()).willReturn(HttpStatus.CREATED);
		given(subject.addDeploymentParameters(principal, input)).willReturn(response);
		ResponseEntity<?> responseReturned = subject.addDeploymentParameters(principal, input);
		assertThat(responseReturned.getStatusCode() , is(HttpStatus.CREATED));
	}

	@Test(expected = InvalidConfigurationDeploymentParametersException.class)
	public void addDeploymentParametersInvalidName(){

		getPrincipal();
		getAccount();
		String cdpsName = "cdps..";
		ConfigurationDeploymentParameters one = new ConfigurationDeploymentParameters(cdpsName, account);
		ConfigurationDeploymentParametersResource input = new ConfigurationDeploymentParametersResource(one);
		given(subject.addDeploymentParameters(principal, input)).willCallRealMethod();
		subject.addDeploymentParameters(principal, input);
	}

	@Test
	public void testGetSharedCdpByAccount(){

		getPrincipal();
		getRequest();
		getAccount();
		getDeploymentParametersResource();
		Set<ConfigurationDeploymentParameters> cdps = new HashSet<>();
		ConfigurationDeploymentParameters cdp = getDeploymentParameters().get(0);
		cdps.add(cdp);
		given(tokenHandler.parseUserFromToken(Mockito.anyString())).willReturn(mock(User.class));
		given(cdpsService.getSharedDeploymentParametersByAccount(Mockito.any(Account.class), Mockito.anyString(),Mockito.any(User.class)))
		.willReturn(cdps);
		Mockito.when(subject.getSharedConfigurationDeploymentParametersByAccount(request, principal)).thenCallRealMethod();
		Resources resources = subject.getSharedConfigurationDeploymentParametersByAccount(request, principal);
		assertEquals(1, resources.getContent().size());
	}

	@Test
	public void testGetSharedConfigurationByAccountReturnsEmpty(){

		getPrincipal();
		getRequest();
		getAccount();
		Set<Configuration> configurations = new HashSet<>();
		Configuration configuration = this.getConfigurations().get(0);
		configurations.add(configuration);
		given(tokenHandler.parseUserFromToken(Mockito.anyString())).willReturn(null);
		given(this.configurationService.getSharedConfigurationsByAccount(Mockito.any(Account.class), Mockito.anyString(),Mockito.any(User.class)))
		.willCallRealMethod();
		Mockito.when(subject.getSharedConfigurationsByAccount(null, request,principal)).thenCallRealMethod();
		Resources resources = subject.getSharedConfigurationsByAccount(null, request, principal);
		assertEquals(0, resources.getContent().size());
	}

	@Test
	public void testGetSharedConfigurationByAccount(){

		getPrincipal();
		getRequest();
		getAccount();
		Set<Configuration> configurations = new HashSet<>();
		Configuration configuration = this.getConfigurations().get(0);
		configurations.add(configuration);
		given(tokenHandler.parseUserFromToken(Mockito.anyString())).willReturn(mock(User.class));
		given(this.configurationService.getSharedConfigurationsByAccount(Mockito.any(Account.class), Mockito.anyString(),Mockito.any(User.class)))
		.willReturn(configurations);
		List<ConfigurationResource> configurationResourceList = new ArrayList<>();
		configurationResourceList.add(getConfigurationResource());
		given(this.configurationService.checkObsoleteConfigurations(configurationResourceList, account, cdpsCopyService))
		.willReturn(configurationResourceList);
		given(configurationService.createConfigurationResource(configurations)).willReturn(configurationResourceList);
		Mockito.when(subject.getSharedConfigurationsByAccount(null, request, principal)).thenCallRealMethod();
		Resources resources = subject.getSharedConfigurationsByAccount(null, request, principal);
		assertEquals(1, resources.getContent().size());
	}

	@Test
	public void testGetSharedConfigurationByName() throws Exception{
		getPrincipal();
		getRequest();
		getAccount();
		Configuration configuration = this.getConfigurations().get(0);
		ConfigDeploymentParamsCopy cdpCopy = mock(ConfigDeploymentParamsCopy.class);
		given(tokenHandler.parseUserFromToken(Mockito.anyString())).willReturn(mock(User.class));
		given(this.configurationService.getSharedConfigurationByName(Mockito.any(Account.class), Mockito.anyString(),Mockito.any(User.class), Mockito.anyString()))
		.willReturn(configuration);
		given(cdpsCopyService.findByConfigurationDeploymentParametersReference(Mockito.anyString())).willReturn(cdpCopy);
		given(this.configurationService.findByNameAndAccountUsername(Mockito.anyString(), Mockito.anyString())).willReturn(configuration);
		Mockito.when(subject.getSharedByName(request, principal, configuration.getName())).thenCallRealMethod();
		ConfigurationResource resource = subject.getSharedByName(request, principal, configuration.getName());
		assertEquals(resource.getName(), configuration.getName());
	}

	@Test(expected = ConfigurationNotFoundException.class)
	public void testGetSharedConfigurationByNameThrowsException() throws Exception{

		getPrincipal();
		getRequest();
		getAccount();
		Configuration configuration = this.getConfigurations().get(0);
		given(tokenHandler.parseUserFromToken(Mockito.anyString())).willReturn(null);
		given(this.configurationService.getSharedConfigurationByName(Mockito.any(Account.class), Mockito.anyString(),Mockito.any(User.class), Mockito.anyString()))
		.willReturn(null);
		ConfigDeploymentParamsCopy cdpCopy = mock(ConfigDeploymentParamsCopy.class);
		given(cdpsCopyService.findByConfigurationDeploymentParametersReference(Mockito.anyString())).willReturn(null);
		Mockito.when(subject.getSharedByName(request, principal, configuration.getName())).thenCallRealMethod();
		subject.getSharedByName(request, principal, configuration.getName());
	}

	@Test
	public void testGetSharedCdpByAccountAndApplicationName(){

		getPrincipal();
		getRequest();
		getAccount();
		getDeploymentParametersResource();
		Set<ConfigurationDeploymentParameters> cdps = new HashSet<>();
		ConfigurationDeploymentParameters cdp = getDeploymentParameters().get(0);
		given(tokenHandler.parseUserFromToken(Mockito.anyString())).willReturn(mock(User.class));
		given(cdpsService.getSharedApplicationByDeploymentParametersName(Mockito.any(Account.class), Mockito.anyString(),Mockito.any(User.class), Mockito.anyString()))
		.willReturn(cdp);
		given(cdpsService.findByNameAndAccountUserName(Mockito.anyString(), Mockito.anyString())).willReturn(cdp);
		Mockito.when(subject.getSharedConfigurationDeploymentParametersByName(request, principal, cdp.getName())).thenCallRealMethod();
		ConfigurationDeploymentParametersResource resource = subject.getSharedConfigurationDeploymentParametersByName(request, principal, cdp.getName());
		assertTrue(resource.getName().equals(cdp.getName()));
	}

	@Test(expected = ConfigurationDeploymentParametersNotFoundException.class)
	public void testGetSharedCdpByAccountAndApplicationNameReturnsNull(){

		getPrincipal();
		getRequest();
		getAccount();
		getDeploymentParametersResource();
		Set<ConfigurationDeploymentParameters> cdps = new HashSet<>();
		ConfigurationDeploymentParameters cdp = getDeploymentParameters().get(0);
		given(tokenHandler.parseUserFromToken(Mockito.anyString())).willReturn(mock(User.class));
		given(cdpsService.getSharedApplicationByDeploymentParametersName(Mockito.any(Account.class), Mockito.anyString(),Mockito.any(User.class), Mockito.anyString()))
		.willReturn(null);
		Mockito.when(subject.getSharedConfigurationDeploymentParametersByName(request, principal, cdp.getName())).thenCallRealMethod();
		ConfigurationDeploymentParametersResource resource = subject.getSharedConfigurationDeploymentParametersByName(request, principal, cdp.getName());
	}

	@Test
	public void testGetSharedCdpByAccountNullCdps(){
		getPrincipal();
		getRequest();
		getAccount();
		getDeploymentParametersResource();
		Set<ConfigurationDeploymentParameters> cdps = new HashSet<>();
		ConfigurationDeploymentParameters cdp = getDeploymentParameters().get(0);
		given(tokenHandler.parseUserFromToken(Mockito.anyString())).willReturn(mock(User.class));
		given(cdpsService.getSharedDeploymentParametersByAccount(Mockito.any(Account.class), Mockito.anyString(),Mockito.any(User.class)))
		.willReturn(cdps);
		Mockito.when(subject.getSharedConfigurationDeploymentParametersByAccount(request, principal)).thenCallRealMethod();
		Resources resources = subject.getSharedConfigurationDeploymentParametersByAccount(request, principal);
		assertEquals(0, resources.getContent().size());
	}

	@Test
	public void testUpdate(){
		getPrincipal();
		getAccount();
		String configName = "someName";
		Configuration configuration = mock(Configuration.class);
		given(configuration.getAccount()).willReturn(account);
		ConfigurationDeploymentParameters cdp = mock(ConfigurationDeploymentParameters.class);
		given(cdp.getId()).willReturn(1L);
		given(configurationService.update(Mockito.any(Account.class),
				Mockito.any(Principal.class), Mockito.any(ConfigurationResource.class), 
				Mockito.anyString(), Mockito.any(ConfigDeploymentParamsCopyService.class))).willReturn(configuration);
		ConfigurationResource configResource = getConfigurationResource();
		given(subject.update(principal,configResource , configName)).willCallRealMethod();
		ConfigDeploymentParamsCopy cdpCopy = new ConfigDeploymentParamsCopy(getDeploymentParameters().get(0));
		cdpCopy.setId(1L);
		given(cdpsCopyService.findByConfigurationDeploymentParametersReference(Mockito.anyString())).willReturn(cdpCopy);
		ResponseEntity<?> response = subject.update(principal, configResource, configName);
		assertTrue(response.getStatusCode().equals(HttpStatus.OK));
	}

	private void getPrincipal(){
		given(principal.getName()).willReturn(principalName);
	}

	private void getAccount(){
		given(accountService.findByUsername(principalName)).willReturn(account);
		given(account.getUsername()).willReturn(principalName);
	}

	private List<ConfigurationDeploymentParameters> getDeploymentParameters(){
		String cdpsName = "cdps";
		List<ConfigurationDeploymentParameters> cdps = new ArrayList<>();
		ConfigurationDeploymentParameters one  = new ConfigurationDeploymentParameters(cdpsName, account );
		one.setId(1L);
		cdps.add(one);
		return cdps;
	}

	private List<ConfigDeploymentParamsCopy> getDeploymentParametersCopy(){
		String cdpsName = "cdps";
		List<ConfigDeploymentParamsCopy> cdps = new ArrayList<>();
		ConfigDeploymentParamsCopy one  = new ConfigDeploymentParamsCopy(getDeploymentParameters().get(0) );
		cdps.add(one);
		return cdps;
	}

	private ConfigurationDeploymentParametersResource getDeploymentParametersResource(){
		String cdpsName = "cdps";
		ConfigurationDeploymentParameters one  = new ConfigurationDeploymentParameters(cdpsName, account);
		ConfigurationDeploymentParametersResource cdpResource = new ConfigurationDeploymentParametersResource(one);
		return cdpResource;

	}

	private List<Configuration> getConfigurations(){
		String configName = "config";
		String cppName = "cppName";
		String sshKey = "sshKey";
		String cppReference = "cppReference";
		List<Configuration> configurations = new ArrayList<>();
		ConfigDeploymentParamsCopy cdpCopy = new ConfigDeploymentParamsCopy(getDeploymentParameters().get(0));
		Configuration one  = new Configuration(configName, account, cppName, cppReference, sshKey, null, null, cdpCopy );
		configurations.add(one);
		return configurations;
	}

	private List<Configuration> getInvalidConfigurations(){
		String configName = "";
		String cppName = "cppName";
		String sshKey = "sshKey";
		String cppReference = "cppReference";
		List<Configuration> configurations = new ArrayList<>();
		ConfigDeploymentParamsCopy cdpCopy = new ConfigDeploymentParamsCopy(getDeploymentParameters().get(0));
		Configuration one  = new Configuration(configName, account, cppName, cppReference,  sshKey, null, null, cdpCopy );
		cdpCopy.setId(1L);
		configurations.add(one);
		return configurations;
	}

	private ConfigurationResource getConfigurationResource(){
		ConfigurationResource configResource = new ConfigurationResource(getConfigurations().get(0), cppCopy);
		return configResource ;
	}

	private ConfigurationResource getInvalidConfigurationResource(){
		ConfigurationResource configResource = new ConfigurationResource(getInvalidConfigurations().get(0), cppCopy);
		return configResource ;
	}

	private void getRequest(){
		given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn(tokenArray);
	}
}
