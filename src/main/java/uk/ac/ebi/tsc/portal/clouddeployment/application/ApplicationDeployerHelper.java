package uk.ac.ebi.tsc.portal.clouddeployment.application;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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

    // Terraform version
    private static final String version1_0_4 = "1.0.4";

	private static final String OS_FLOATING_IP = "floating_ip";
	private static final String ERROR_MESSAGE = "error(s) occurred";
	private static final String IMAGE_NAME_ERROR_MESSAGE = "Error resolving image name";
	private static final String TIMEOUT_ERROR_MESSAGE = "Error waiting for instance";
	private static final String QUOTA_EXCEEDED_ERROR_MESSAGE = "Quota exceeded for ";

    private static final String terraformStateKeyAddress = "address";
    private static final String terraformStateKeyId = "id";
    private static final String terraformStateKeyOutputs = "outputs";
    private static final String terraformStateKeyResources = "resources";
    private static final String terraformStateKeyRootModule = "root_module";
    private static final String terraformStateKeyValue = "value";
    private static final String terraformStateKeyValues = "values";
    private static final String terraformStateResourceAddress = "openstack_compute_floatingip_associate_v2.instance_public_ip";

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

    // See this class's JUnit test routines for sample outputs.
    @SuppressWarnings("rawtypes")
    private static StateFromTerraformOutput terraformStateFromJson(final JSONObject terraformOutputJson,
                                                                   final Map<String, String> applicationOutputs,
                                                                   final Logger logger) {

        logger.debug("terraformStateFromJson() : Parsing from json output");

        final String terraformVersion = (String) terraformOutputJson.get("terraform_version");
        if (!version1_0_4.equals(terraformVersion)) {
            // This processing is based on version 1.0.4, so no guarantees with any other version!
            logger.warn("terraformStateFromJson() : Expecting terraform version {} output, but encountered version {}",
                        version1_0_4, terraformVersion);
        }

        final StateFromTerraformOutput stateFromTerraformOutput = new StateFromTerraformOutput();

        try {
            final JSONObject values = (JSONObject) terraformOutputJson.get(terraformStateKeyValues);
            // Extract application "outputs"
            if (applicationOutputs != null) {
                final JSONObject jsonOutputs = (JSONObject) values.get(terraformStateKeyOutputs);
                if (!jsonOutputs.isEmpty()) {
                    for (final Iterator outputsIterator = jsonOutputs.keySet().iterator();
                         outputsIterator.hasNext();) {
                        final String outputsKey = (String) outputsIterator.next();
                        if (applicationOutputs.containsKey(outputsKey)) {
                            final JSONObject outputsObject = (JSONObject) jsonOutputs.get(outputsKey);
                            final String outputsValue = (String) outputsObject.get(terraformStateKeyValue);

                            applicationOutputs.put(outputsKey, outputsValue);
                        }
                    }
                }
            }

            /*
             * Load StateFromTerraformOutput bean properties.
             * Note: Historically only the "id" and "accessIp" properties have been assigned. 
             */
            final JSONObject rootModule = (JSONObject) values.get(terraformStateKeyRootModule);
            final JSONArray resources = (JSONArray) rootModule.get(terraformStateKeyResources);
            boolean foundInstancePublicIp = false;

            for (final Iterator resourcesIterator = resources.iterator();
                 resourcesIterator.hasNext() && !foundInstancePublicIp;) {
                 final JSONObject resourceObject = (JSONObject) resourcesIterator.next();
                final String resourceAddress = (String) resourceObject.get(terraformStateKeyAddress);
                logger.debug("Checking resource address {}", resourceAddress);

                if (terraformStateResourceAddress.equals(resourceAddress)) {
                    foundInstancePublicIp = true;

                    final JSONObject resourceValues = (JSONObject) resourceObject.get(terraformStateKeyValues);
                    if (resourceValues != null) {
                        final String id = (String) resourceValues.get(terraformStateKeyId);
                        final String floatingIp = (String) resourceValues.get(OS_FLOATING_IP);

                        stateFromTerraformOutput.setId(id);
                        stateFromTerraformOutput.setAccessIp(floatingIp);
                    }
                }
            }
        } catch (NullPointerException e) {
            // Loose capturing of situations where expected json object not found by key
            logger.error("terraformStateFromJson() : NullPointerException querying json content - verify file content");
        }

        return stateFromTerraformOutput;
    }

    /**
     * Process the output of a run of whatever's generated by {@code state.sh}'s 
     * {@code terraform show} command, e.g. e.g. https://github.com/EMBL-EBI-TSI/cpa-bioexcel-cwl/blob/master/ostack/state.sh
     * <p>
     * See this class's JUnit test routines for sample outputs.
     * 
     * @param output The output of the "{@code terraform show ...}" command.
     * @param outputs Collection of the application's expected outputs (for dynamic populating, e.g.
     *                {@code external_ip}, {@code ssh_command}, etc.)
     * @param logger Logger
     * @return Bean containing state information.
     */
     public static StateFromTerraformOutput terraformStateFromString(String terraformOutput,
                                                                     Map<String, String> outputs,
                                                                     Logger logger) {

        logger.info("The whole terraform state is: " + terraformOutput);

        // Note: JSONParser is NOT thread-safe!
        final JSONParser jsonParser = new JSONParser();
        try {
            return terraformStateFromJson((JSONObject) jsonParser.parse(terraformOutput),
                                           outputs, logger); 
        } catch (final ParseException parseException) {
            logger.info("terraformStateFromString() : ParseException thrown, so assuming (legacy!) non-json format");

            return terraformStateFromLegacy(terraformOutput, outputs, logger);
        }

	}

     // See this class's JUnit test routines for sample outputs.
     private static StateFromTerraformOutput terraformStateFromLegacy(final String legacyOutput,
                                                                      final Map<String, String> applicationOutputs,
                                                                      final Logger logger) {

         StateFromTerraformOutput stateFromTerraformOutput = new StateFromTerraformOutput();

         String[] lines = legacyOutput.split(System.getProperty("line.separator"));

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
         if (applicationOutputs != null) {
             getOutputs(lines, applicationOutputs, logger);
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
				logger.debug("There is a match. " + key + " = " + outputs.get(key));
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
