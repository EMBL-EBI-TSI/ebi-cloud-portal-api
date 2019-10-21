package uk.ac.ebi.tsc.portal.clouddeployment.application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import uk.ac.ebi.tsc.portal.api.application.controller.InvalidApplicationInputValueException;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationInput;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopy;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopyField;
import uk.ac.ebi.tsc.portal.api.deployment.controller.DeploymentAssignedInputResource;
import uk.ac.ebi.tsc.portal.clouddeployment.model.ErrorFromTerraformOutput;
import uk.ac.ebi.tsc.portal.clouddeployment.model.MachineSpecs;
import uk.ac.ebi.tsc.portal.clouddeployment.model.StateFromTerraformOutput;
import uk.ac.ebi.tsc.portal.clouddeployment.model.terraform.TerraformModule;
import uk.ac.ebi.tsc.portal.clouddeployment.model.terraform.TerraformResource;
import uk.ac.ebi.tsc.portal.clouddeployment.model.terraform.TerraformState;
import uk.ac.ebi.tsc.portal.usage.deployment.model.DeploymentDocument;

/**
 * Created by jdianes on 26/09/2018.
 */
@Component
public class ApplicationDeployerHelper {

	private static final String OS_FLOATING_IP = "floating_ip";
	private static final String ERROR_MESSAGE = "error(s) occurred";
	private static final String IMAGE_NAME_ERROR_MESSAGE = "Error resolving image name";
	private static final String TIMEOUT_ERROR_MESSAGE = "Error waiting for instance";
	private static final String QUOTA_EXCEEDED_ERROR_MESSAGE = "Quota exceeded for ";

	public static String getOutputFromFile(File file, Logger logger) {
		logger.info("Retrieving output for process from " + file.getAbsolutePath());

		String output = null;
		try {
			output = new Scanner(file).useDelimiter("\\Z").next();
		} catch (FileNotFoundException e) {
			logger.error("Can't read output from file " + file.getAbsolutePath());
			e.printStackTrace();
		}

		return output;
	}


	public static void addGenericProviderCreds(Map<String, String> env, CloudProviderParamsCopy cloudProviderCredentialsCopy, Logger logger) {
		logger.info("Setting Cloud Provider Parameters Copy for " + cloudProviderCredentialsCopy.getName());

		for (CloudProviderParamsCopyField cloudProviderParametersCopyField : cloudProviderCredentialsCopy.getFields()) {
			logger.info("Setting " + cloudProviderParametersCopyField.getKey());
			env.put(cloudProviderParametersCopyField.getKey(), cloudProviderParametersCopyField.getValue());
		}
	}

	public static StateFromTerraformOutput terraformStateFromString(String output, Map<String, String> outputs, Logger logger) throws IOException {

		logger.info("The whole terraform state is: " + output);

		StateFromTerraformOutput stateFromTerraformOutput = new StateFromTerraformOutput();

		String[] lines = output.split(System.getProperty("line.separator"));

		// Read Id, it should be the first ID (TODO improve this later)
		stateFromTerraformOutput.setId(lines[1].replaceAll(" ","").split("=")[1]);
		// look for the IP
		Pattern osFloatingIpPattern = Pattern.compile(OS_FLOATING_IP);
		boolean ipFound = false;
		int i=1;
		while (!ipFound && i<lines.length) {
			String line = lines[i].replaceAll(" ","");
			Matcher osFloatingIpMatcher = osFloatingIpPattern.matcher(line);
			if (osFloatingIpMatcher.lookingAt()) {
				// Read IP
				String ip = line.split("=")[1].replaceAll("\n", "").replaceAll("\r", "");
				logger.debug("There is a match. IP = " + ip);
				stateFromTerraformOutput.setAccessIp(ip);
				ipFound = true;
			}
			i++;
		}
		// parse for any other outputs
		if (outputs != null) {
			getOutputs(lines, outputs, logger);
		}

		return stateFromTerraformOutput;
	}

	public static void getOutputs(String[] lines, Map<String, String> outputs, Logger logger) {
		for (int i = 0; i<lines.length; i++) {
			String line = lines[i];
			String[] lineSplit = line.split("=");
			String key = lineSplit[0].trim();
			if ( outputs.containsKey(key) ) {
				outputs.put(key, lineSplit[1].replaceAll("\n", "").replaceAll("\r", "").replaceAll("\\[0m","").trim());
				logger.debug("There is a match. " + lineSplit[0] + " = " + outputs.get(lineSplit[0]));
			}
		}
	}

	public static ErrorFromTerraformOutput errorFromTerraformOutput(String terraformOutput, Logger logger) {
		ErrorFromTerraformOutput res = new ErrorFromTerraformOutput();
		res.setErrorMessage("unknown");

		// look for specific error message

		if (terraformOutput.contains(TIMEOUT_ERROR_MESSAGE)) {
			logger.info("Timeout error");
			res.setErrorMessage("timeout");
			return res;
		}

		if (terraformOutput.contains(QUOTA_EXCEEDED_ERROR_MESSAGE)) {
			logger.info("Quota exceeded error");
			res.setErrorMessage("quota exceeded");
			return res;
		}

		if (terraformOutput.contains(IMAGE_NAME_ERROR_MESSAGE)) {
			logger.info("Error resolving image name");
			res.setErrorMessage("unresolvable image name");
			return res;
		}

		return res;
	}

	public static long updateResourceConsumptionFromTerraformState(TerraformState terraformState, DeploymentDocument deploymentDocument) {
		long res = 0;
		if (terraformState.modules!=null && terraformState.modules.length>0) {
			Iterator<TerraformModule> itModules = Arrays.stream(terraformState.modules).iterator();
			while (itModules.hasNext()) {
				TerraformModule terraformModule = itModules.next();
				Iterator<Map.Entry<String, TerraformResource>> it = terraformModule.resources.entrySet().iterator();
				while (it.hasNext()) {
					TerraformResource terraformResource = it.next().getValue();
					if (terraformResource.type.equals("openstack_compute_instance_v2")) {
						deploymentDocument.setInstanceCount(deploymentDocument.getInstanceCount()+1);
						deploymentDocument.setTotalVcpus(
								deploymentDocument.getTotalVcpus()
								+ MachineSpecs.fromFlavourName(terraformResource.primary.attributes.get("flavor_name")).getvCPUSs()
								);
						deploymentDocument.setTotalRamGb(
								deploymentDocument.getTotalRamGb()
								+ MachineSpecs.fromFlavourName(terraformResource.primary.attributes.get("flavor_name")).getRamGb()
								);
						deploymentDocument.setTotalDiskGb(
								deploymentDocument.getTotalDiskGb()
								+ MachineSpecs.fromFlavourName(terraformResource.primary.attributes.get("flavor_name")).getDiskSpaceGb()
								);
					} else if (terraformResource.type.equals("google_compute_instance")) {
						deploymentDocument.setInstanceCount(deploymentDocument.getInstanceCount()+1);
						deploymentDocument.setTotalVcpus(
								deploymentDocument.getTotalVcpus()
								+ MachineSpecs.fromFlavourName(terraformResource.primary.attributes.get("machine_type")).getvCPUSs()
								);
						deploymentDocument.setTotalRamGb(
								deploymentDocument.getTotalRamGb()
								+ MachineSpecs.fromFlavourName(terraformResource.primary.attributes.get("machine_type")).getRamGb()
								);
						deploymentDocument.setTotalDiskGb(
								deploymentDocument.getTotalDiskGb()
								+ MachineSpecs.fromFlavourName(terraformResource.primary.attributes.get("machine_type")).getDiskSpaceGb()
								);
					} else if (terraformResource.type.equals("aws_instance")) {
						deploymentDocument.setInstanceCount(deploymentDocument.getInstanceCount()+1);
						deploymentDocument.setTotalVcpus(
								deploymentDocument.getTotalVcpus()
								+ MachineSpecs.fromFlavourName(terraformResource.primary.attributes.get("instance_type")).getvCPUSs()
								);
						deploymentDocument.setTotalRamGb(
								deploymentDocument.getTotalRamGb()
								+ MachineSpecs.fromFlavourName(terraformResource.primary.attributes.get("instance_type")).getRamGb()
								);
						deploymentDocument.setTotalDiskGb(
								deploymentDocument.getTotalDiskGb()
								+ MachineSpecs.fromFlavourName(terraformResource.primary.attributes.get("instance_type")).getDiskSpaceGb()
								);
					} else if (terraformResource.type.equals("azurerm_virtual_machine")) {
						deploymentDocument.setInstanceCount(deploymentDocument.getInstanceCount()+1);
						deploymentDocument.setTotalVcpus(
								deploymentDocument.getTotalVcpus()
								+ MachineSpecs.fromFlavourName(terraformResource.primary.attributes.get("vm_size")).getvCPUSs()
								);
						deploymentDocument.setTotalRamGb(
								deploymentDocument.getTotalRamGb()
								+ MachineSpecs.fromFlavourName(terraformResource.primary.attributes.get("vm_size")).getRamGb()
								);
						deploymentDocument.setTotalDiskGb(
								deploymentDocument.getTotalDiskGb()
								+ MachineSpecs.fromFlavourName(terraformResource.primary.attributes.get("vm_size")).getDiskSpaceGb()
								);
					}
				}
			}
		}
		return res;
	}


	public Map<String, String> validateInputNameandValues(
			Collection<DeploymentAssignedInputResource> assignedInputs,
			Application application) throws InvalidApplicationInputValueException  {

		Collection<ApplicationInput> applicationInputs = application.getInputs();
		
		List<String> applicationInputNames = applicationInputs.stream().map(input -> input.getName()).collect(Collectors.toList());
		
		assignedInputs.stream().forEach(assignedInput -> {
			
			//if the input value is valid for the application input
			applicationInputs.
					stream().
					filter(a -> a.getName().equals(assignedInput.getInputName()))
					.findFirst()
					.ifPresent(applicationInput -> {
						if(!applicationInput.getValues().isEmpty() && !applicationInput.getValues().contains(assignedInput.getAssignedValue())){
							throw new InvalidApplicationInputValueException(assignedInput.getInputName(), assignedInput.getAssignedValue());
						}
					});
			
		});
		
		//if inputs are all valid, then return them
		return assignedInputs
				.stream()
				.collect(Collectors.toMap(a -> a.getInputName(), a -> a.getAssignedValue()));
	}
}
