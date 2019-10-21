package uk.ac.ebi.tsc.portal.api.deployment.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import uk.ac.ebi.tsc.portal.api.application.controller.InvalidApplicationInputValueException;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationInput;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDeployerHelper;

@RunWith(SpringRunner.class)
@WebAppConfiguration
public class ApplicationDeployerHelperTest {

	@MockBean
	ApplicationDeployerHelper helper;

	@Before
	public void setUp() {

	}

	@Test
	public void testValidInput() {
		Application application = mock(Application.class);

		//application inputs
		ApplicationInput inputOne = mock(ApplicationInput.class);
		when(inputOne.getName()).thenReturn("inputOne");
		String[] values = {"a","d"};
		when(inputOne.getValues()).thenReturn(Arrays.asList(values));
		List<ApplicationInput> inputs = new ArrayList();
		when(application.getInputs()).thenReturn(inputs);
		inputs.add(inputOne);

		//actual chosen inputs
		List<DeploymentAssignedInputResource> assignedInputs = new ArrayList();
		DeploymentAssignedInputResource assignedInput = mock(DeploymentAssignedInputResource.class);
		when(assignedInput.getInputName()).thenReturn("inputOne");
		when(assignedInput.getAssignedValue()).thenReturn("a");
		assignedInputs.add(assignedInput);
		when(helper.validateInputNameandValues(assignedInputs, application)).thenCallRealMethod();
		Map<String, String> returnedInputs = helper.validateInputNameandValues(assignedInputs, application);
		assertEquals(returnedInputs.size(),1);
	}

	@Test
	public void testGetOutputsWithoutSpaces() {
		String outputLog = "openstack_networking_floatingip_v2.floatingip:\n" +
				"  id = 23d88c50-bbbe-4684-b6ae-869179cc1254\n\n" +
				"Outputs:\n" +
				"\n" +
				"ip = 193.62.1.1\n" +
				"command = cwltool\n" +
				"user =  ubuntu";
		String ip = "193.62.1.1";
		String command = "cwltool";
		String user = "ubuntu";

		Map<String, String> outputs = new HashMap<>();
		outputs.put("ip","");
		outputs.put("command","");
		outputs.put("user","");
		Logger logger = mock(Logger.class);
		String[] lines = outputLog.split(System.getProperty("line.separator"));
		ApplicationDeployerHelper.getOutputs(lines,outputs,logger);
		assertEquals(outputs.get("ip"), ip);
		assertEquals(outputs.get("command"), command);
		assertEquals(outputs.get("user"), user);
	}

	@Test
	public void testGetOutputsWithTrailingSpaces() {
		String outputLog = "openstack_networking_floatingip_v2.floatingip:\n" +
				"  id = 23d88c50-bbbe-4684-b6ae-869179cc1254\n\n" +
				"Outputs:\n" +
				"\n" +
				"ip = 193.62.1.1 \n" +
				"command = cwltool\n" +
				"user = ubuntu\u001B";
		String ip = "193.62.1.1";
		String command = "cwltool";
		String user = "ubuntu";

		Map<String, String> outputs = new HashMap<>();
		outputs.put("ip","");
		outputs.put("command","");
		outputs.put("user","");
		Logger logger = mock(Logger.class);
		String[] lines = outputLog.split(System.getProperty("line.separator"));
		ApplicationDeployerHelper.getOutputs(lines,outputs,logger);
		assertEquals(outputs.get("ip"), ip);
		assertEquals(outputs.get("command"), command);
		assertEquals(outputs.get("user"), user);
	}

	@Test
	public void testGetOutputsWithSpaceinbetween() {
		String outputLog = "openstack_networking_floatingip_v2.floatingip:\n" +
				"  id = 23d88c50-bbbe-4684-b6ae-869179cc1254\n\n" +
				"Outputs:\n" +
				"\n" +
				"external_ip = 193.62.1.1\n" +
				"ssh_command = ssh ubuntu@193.62.1.1\n" +
				"ssh_user = ubuntu\u001B";
		String external_ip = "193.62.1.1";
		String ssh_command = "ssh ubuntu@193.62.1.1";
		String ssh_user = "ubuntu";

		Map<String, String> outputs = new HashMap<>();
		outputs.put("external_ip","");
		outputs.put("ssh_command","");
		outputs.put("ssh_user","");
		Logger logger = mock(Logger.class);
		String[] lines = outputLog.split(System.getProperty("line.separator"));
		ApplicationDeployerHelper.getOutputs(lines,outputs,logger);
		assertEquals(outputs.get("external_ip"), external_ip);
		assertEquals(outputs.get("ssh_command"), ssh_command);
		assertEquals(outputs.get("ssh_user"), ssh_user);
	}

	@Test(expected = InvalidApplicationInputValueException.class)
	public void testInvalidInputValueException() {
		Application application = mock(Application.class);

		//application inputs
		ApplicationInput inputOne = mock(ApplicationInput.class);
		when(inputOne.getName()).thenReturn("inputOne");
		String[] values = {"a","d"};
		when(inputOne.getValues()).thenReturn(Arrays.asList(values));
		List<ApplicationInput> inputs = new ArrayList();
		when(application.getInputs()).thenReturn(inputs);
		inputs.add(inputOne);

		//actual chosen inputs
		List<DeploymentAssignedInputResource> assignedInputs = new ArrayList();
		DeploymentAssignedInputResource assignedInput = mock(DeploymentAssignedInputResource.class);
		when(assignedInput.getInputName()).thenReturn("inputOne");
		when(assignedInput.getAssignedValue()).thenReturn("h");
		assignedInputs.add(assignedInput);
		when(helper.validateInputNameandValues(assignedInputs, application)).thenCallRealMethod();
		helper.validateInputNameandValues(assignedInputs, application);
	}
}
