package uk.ac.ebi.tsc.portal.api.deployment.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
