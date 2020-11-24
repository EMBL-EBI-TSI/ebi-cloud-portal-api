package uk.ac.ebi.tsc.portal.usage.tracker;

import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopy;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopyRepository;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyService;
import uk.ac.ebi.tsc.portal.api.deployment.repo.Deployment;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentStatusEnum;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentNotFoundException;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.encryptdecrypt.security.EncryptionService;
import uk.ac.ebi.tsc.portal.usage.deployment.model.DeploymentDocument;
import uk.ac.ebi.tsc.portal.usage.deployment.model.ParameterDocument;
import uk.ac.ebi.tsc.portal.usage.deployment.service.DeploymentIndexService;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Component
public class DeploymentStatusUpdate implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(DeploymentStatusUpdate.class);

	private DeploymentIndexService deploymentIndexService;
	private DeploymentService deploymentService;
	private final EncryptionService encryptionService;
	private final CloudProviderParamsCopyRepository cloudProviderParametersCopyRepository;
	private final CloudProviderParamsCopyService cloudProviderParametersCopyService;

	public DeploymentStatusUpdate(DeploymentIndexService deploymentIndexService, 
			DeploymentService deploymentService,
			CloudProviderParamsCopyRepository cloudProviderParametersCopyRepository,
			EncryptionService encryptionService,
			CloudProviderParamsCopyService cloudProviderParametersCopyService) {
		this.deploymentIndexService = deploymentIndexService;
		this.deploymentService = deploymentService;
		this.cloudProviderParametersCopyRepository = cloudProviderParametersCopyRepository;
		this.encryptionService = encryptionService;
		this.cloudProviderParametersCopyService = cloudProviderParametersCopyService;
	}

	@Override
	public void run() {
		if (this.deploymentIndexService == null) {
			logger.error("DeploymentIndex service not available. Cause: NOT INSTANTIATED");
		} /*else {

			try {
				Collection<DeploymentDocument> deploymentDocuments =
						this.deploymentIndexService.findByStatus(DeploymentStatusEnum.RUNNING.toString());

				logger.debug("Updating status and total running time for " + deploymentDocuments.size() + " indexed deployments");

				deploymentDocuments.stream().forEach(deploymentDocument -> {
					// Check if deployment is still running
					try {
						Deployment theDeployment = this.deploymentService.findByReference(deploymentDocument.getDeploymentReference());
						if (theDeployment.getDeploymentStatus().getStatus() != DeploymentStatusEnum.RUNNING) {
							logger.debug("Deployment " + theDeployment.getReference() + " is " + theDeployment.getDeploymentStatus().getStatus());
							deploymentDocument.setStatus(theDeployment.getDeploymentStatus().getStatus().toString());
							this.deploymentIndexService.save(deploymentDocument);
						} else { // If still running, update total running time
							deploymentDocument.setStatus(null); // keep from changing the status if updated somewhere else
							if ( deploymentDocument.getDeployedTime()!=null ) {
								deploymentDocument.setTotalRunningTime(
										System.currentTimeMillis() - deploymentDocument.getDeployedTime().getTime()
										);
							}
							this.deploymentIndexService.saveTotalRunningTime(
									deploymentDocument.getDeploymentReference(),
									deploymentDocument.getTotalRunningTime());
						}
					} catch (DeploymentNotFoundException dnfe) {
						logger.error("Deployment " + deploymentDocument.getDeploymentReference() + " is now MISSING.", dnfe);
						deploymentDocument.setStatus("MISSING");
						this.deploymentIndexService.save(deploymentDocument);
					} catch (RestClientException rce) {
						logger.error("DeploymentIndex service not available. Cause: ", rce);
					} catch (Exception e) {
						logger.error("Unexpected exception. Cause: ", e);
					}
				});
			} catch (RestClientException rce) {
				logger.error("DeploymentIndex service not available. Cause: ", rce);
			} catch (Exception e) {
				logger.error("Unexpected exception. Cause: ", e);
			}

			try {
				Collection<Deployment> runningDeployments = deploymentService.findByDeploymentStatusStatus(DeploymentStatusEnum.RUNNING);

				logger.debug("Updating status and total running time for " + runningDeployments.size() + " RUNNING deployments on DB");

				runningDeployments.stream().forEach(deployment -> {
					try {
						DeploymentDocument deploymentDocument = this.deploymentIndexService.findById(deployment.getReference());
						CloudProviderParamsCopy cloudProviderParametersCopy = this.cloudProviderParametersCopyService.
								findByCloudProviderParametersReference(deployment.getCloudProviderParametersReference());
						if (deploymentDocument == null) { // deployment missing, add it
							deploymentDocument = new DeploymentDocument(
									deployment.getAccount().getEmail(),
									deployment.getReference(),
									deployment.getDeploymentApplication().getName(),
									deployment.getDeploymentApplication().getContact(),
									deployment.getDeploymentApplication().getVersion(),
									cloudProviderParametersCopy.getCloudProvider(),
									cloudProviderParametersCopy.getName());
							deploymentDocument.setStatus(deployment.getDeploymentStatus().getStatus().toString());
							deploymentDocument.setStartedTime(new Date(System.currentTimeMillis())); // TODO we need a started time field on DB
							if (!(deploymentDocument.getStatus().equals(DeploymentStatusEnum.STARTING)
									|| deploymentDocument.getStatus().equals(DeploymentStatusEnum.STARTING_FAILED))) {
								deploymentDocument.setDeployedTime(new Date(System.currentTimeMillis())); // TODO we need a deployed time field on DB
							}
							deploymentDocument.setDeploymentInputs(deployment.assignedInputs.stream().map(
									deploymentAssignedInput -> {
										ParameterDocument pd = new ParameterDocument();
										pd.setKey(deploymentAssignedInput.getInputName());
										pd.setValue(deploymentAssignedInput.getValue());
										return pd;
									}
									).collect(Collectors.toList()));
							deploymentDocument.setDeploymentParameters(deployment.assignedParameters.stream().map(
									deploymentAssignedParameter -> {
										ParameterDocument pd = new ParameterDocument();
										pd.setKey(deploymentAssignedParameter.getParameterName());
										pd.setValue(deploymentAssignedParameter.getParameterValue());
										return pd;
									}
									).collect(Collectors.toList()));

							this.deploymentIndexService.save(deploymentDocument);
						}
					} catch (RestClientException rce) {
						logger.error("DeploymentIndex service not available. Cause: ", rce);
					} catch (Exception e) {
						logger.error("Unexpected exception. Cause: ", e);
					}
				});
			} catch (RestClientException rce) {
				logger.error("DeploymentIndex service not available. Cause: ", rce);
			} catch (Exception e) {
				logger.error("Unexpected exception. Cause: ", e);
			}

		}*/

	}
}
