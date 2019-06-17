package uk.ac.ebi.tsc.portal.usage.tracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopy;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyService;
import uk.ac.ebi.tsc.portal.api.configuration.repo.Configuration;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationService;
import uk.ac.ebi.tsc.portal.api.deployment.repo.*;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentConfigurationService;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.utils.SendMail;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDeployer;
import uk.ac.ebi.tsc.portal.clouddeployment.exceptions.ApplicationDeployerException;
import uk.ac.ebi.tsc.portal.usage.deployment.service.DeploymentIndexService;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Component
public class ConfigurationUsageMonitor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationUsageMonitor.class);

    private final DeploymentIndexService deploymentIndexService;
    private final DeploymentService deploymentService;
    private final ConfigurationService configurationService;
    private final CloudProviderParamsCopyService cloudProviderParamsCopyService;
    private final DeploymentConfigurationService deploymentConfigurationService;
    private final SendMail sendMail;

    private final ApplicationDeployer applicationDeployer;

    public static final double NOTIFICATION_PERIOD = 6.048e+8;

    @Autowired
    public ConfigurationUsageMonitor(DeploymentIndexService deploymentIndexService,
                                     DeploymentService deploymentService,
                                     ConfigurationService configurationService,
                                     CloudProviderParamsCopyService cloudProviderParamsCopyService,
                                     DeploymentConfigurationService deploymentConfigurationService,
                                     ApplicationDeployer applicationDeployer,
                                     SendMail sendMail) {
        this.deploymentIndexService = deploymentIndexService;
        this.deploymentService = deploymentService;
        this.configurationService = configurationService;
        this.cloudProviderParamsCopyService = cloudProviderParamsCopyService;
        this.deploymentConfigurationService = deploymentConfigurationService;
        this.applicationDeployer = applicationDeployer;
        this.sendMail = sendMail;
    }

    @Override
    public void run() {
        if (this.deploymentIndexService == null) {
            logger.error("DeploymentIndex service not available. Cause: NOT INSTANTIATED");
        } else {
            try {
                this.configurationService.findAll().stream().forEach(
                        config -> {
                            double totalConsumption = this.configurationService.getTotalConsumption(config, this.deploymentIndexService);
                            // check hard limit first
                            if (config.getHardUsageLimit() != null && config.getHardUsageLimit() < totalConsumption) {
                                logger.debug("Configuration " + config.getName() + " reached hard usage limit of " + config.getHardUsageLimit() + " (used " + totalConsumption + ")");
                                // we must stop all running deployments associated with this config
                                Collection<Deployment> deployments = this.deploymentService.findByConfigurationReference(config.getReference());
                                deployments.stream().forEach(
                                        deployment -> {
                                            try {
                                                if (deployment.getDeployedTime() != null && deployment.getDeploymentStatus().getStatus().equals(DeploymentStatusEnum.RUNNING)) {
                                                    logger.info("STOPPING deployment " + deployment.getReference() + " owned by " + deployment.getAccount().getEmail());
                                                    this.stopDeployment(deployment);
                                                    this.notifyDeploymentDestroyedOwner(deployment, config);
                                                } else if (deployment.getDeployedTime() != null && deployment.getDeploymentStatus().getStatus().equals(DeploymentStatusEnum.DESTROYING_FAILED)) {
                                                    logger.info("SKIPPING STOPPING of deployment " + deployment.getReference() + " owned by " + deployment.getAccount().getEmail() + " cause it PREVIOUSLY FAILED!");
                                                }
                                            } catch (IOException e) {
                                                logger.error("Error " + e.getStackTrace());
                                                e.printStackTrace();
                                            } catch (ApplicationDeployerException e) {
                                                logger.error("Error " + e.getStackTrace());
                                                e.printStackTrace();
                                            } catch (Exception e) {
                                                logger.error("Error " + e.getStackTrace());
                                                e.printStackTrace();
                                            }
                                        }
                                );
                            } else if (config.getSoftUsageLimit() != null && config.getSoftUsageLimit() < totalConsumption) { // then check soft limit
                                logger.debug("Configuration " + config.getName() + " reached soft usage limit of " + config.getSoftUsageLimit() + " (used " + totalConsumption + ")");
                                Collection<Deployment> deployments = this.deploymentService.findByConfigurationReference(config.getReference());
                                deployments.stream().forEach(
                                        deployment -> {
                                            try {
                                                if (deployment.getDeployedTime() != null && deployment.getDeploymentStatus().getStatus().equals(DeploymentStatusEnum.RUNNING) &&
                                                        (deployment.getLastNotificationTime()==null || (System.currentTimeMillis()>deployment.getLastNotificationTime().getTime()+NOTIFICATION_PERIOD))) {
                                                    logger.info("NOTIFYING deployment " + deployment.getReference() + " owned by " + deployment.getAccount().getEmail());
                                                    logger.info(" deployment " + deployment.getReference() + " owned by " + deployment.getAccount().getEmail());
                                                    this.notifyDeploymentBeyondOwner(deployment, config);
                                                    deployment.setLastNotificationTime(new Timestamp(System.currentTimeMillis()));
                                                    this.deploymentService.save(deployment);
                                                } else if (deployment.getDeployedTime() != null && deployment.getDeploymentStatus().getStatus().equals(DeploymentStatusEnum.DESTROYING_FAILED)) {
                                                    logger.info("SKIPPING STOPPING of deployment " + deployment.getReference() + " owned by " + deployment.getAccount().getEmail() + " cause it PREVIOUSLY FAILED!");
                                                }
                                            } catch (IOException e) {
                                                logger.error("Error " + e.getStackTrace());
                                                e.printStackTrace();
                                            } catch (ApplicationDeployerException e) {
                                                logger.error("Error " + e.getStackTrace());
                                                e.printStackTrace();
                                            } catch (Exception e) {
                                                logger.error("Error " + e.getStackTrace());
                                                e.printStackTrace();
                                            }
                                        }
                                );
                            }
                        }
                );
            } catch (Exception e) {
                logger.error("Unexpected exception. Cause: ", e);
            }
        }
    }

    private void stopDeployment(Deployment theDeployment) throws IOException, ApplicationDeployerException {
        // get credentials decrypted through the service layer
        CloudProviderParamsCopy theCloudProviderParametersCopy;
        theCloudProviderParametersCopy = this.cloudProviderParamsCopyService.findByCloudProviderParametersReference(theDeployment.getCloudProviderParametersReference());

        DeploymentConfiguration deploymentConfiguration = null;
        if(theDeployment.getDeploymentConfiguration() != null){
            deploymentConfiguration = this.deploymentConfigurationService.findByDeployment(theDeployment);
        }

        // Update status
        theDeployment.getDeploymentStatus().setStatus(DeploymentStatusEnum.DESTROYING);
        this.deploymentService.save(theDeployment);

        // Proceed to destroy
        logger.info("DEPLOYER = " + this.applicationDeployer);
        this.applicationDeployer.destroy(
                theDeployment.getDeploymentApplication().getRepoPath(),
                theDeployment.getReference(),
                getCloudProviderPathFromDeploymentApplication(
                        theDeployment.getDeploymentApplication(), theCloudProviderParametersCopy.getCloudProvider()),
                theDeployment.getAssignedInputs(),
                theDeployment.getAssignedParameters(),
                theDeployment.getAttachedVolumes(),
                deploymentConfiguration,
                theCloudProviderParametersCopy
        );
    }

    private String getCloudProviderPathFromDeploymentApplication(DeploymentApplication deploymentApplication, String cloudProvider) {

        logger.info("Getting the path of the cloud provider from deploymentApplication");
        Iterator<DeploymentApplicationCloudProvider> it = deploymentApplication.getCloudProviders().iterator();

        while ( it.hasNext() ) {
            DeploymentApplicationCloudProvider cp = it.next();
            if (cloudProvider.equals(cp.getName())) {
                logger.info("PATH = " + cp.getPath());
                return cp.getPath();
            }
        }

        logger.info("PATH IS NULL");
        return null;
    }

    public void notifyDeploymentDestroyedOwner(Deployment deployment, Configuration configuration) throws Exception {
        Collection<String> toNotify = new LinkedList<>();
        toNotify.add(deployment.getAccount().getEmail());
        toNotify.add(configuration.getAccount().getEmail());

        String message = "The deployment of '" + deployment.getDeploymentApplication().getName() + "' (" + deployment.getReference() + ") was destroyed. \n"
                + "This was because the configuration " + "'" + configuration.name + "'" +" hard usage limit was reached. \n"
                + "You can contact the configuration owner at " + configuration.getAccount().getEmail() + ".";
        try{
            sendMail.send(toNotify, "Deployment " + deployment.getReference() + " destroyed", message );
        } catch (IOException e) {
            logger.error("Failed to send messages to concerned person, regarding destroying deployment");
        }

    }

    public void notifyDeploymentBeyondOwner(Deployment deployment, Configuration configuration) throws Exception {
        Collection<String> toNotify = new LinkedList<>();
        toNotify.add(deployment.getAccount().getEmail());
        toNotify.add(configuration.getAccount().getEmail());

        String message = "The deployment of '" + deployment.getDeploymentApplication().getName() + "' (" + deployment.getReference() + ") has reached the soft usage limit of " +
                " configuration " + "'" + configuration.name + "'. The deployment will be running until is destroyed manually or reaches a hard usage limit. \n"
                    + "You can contact the configuration owner at " + configuration.getAccount().getEmail() + ".";
        try{
            sendMail.send(toNotify, "Deployment " + deployment.getReference() + " usage limit", message );
        } catch (IOException e) {
            logger.error("Failed to send messages to concerned person, regarding destroying deployment");
        }

    }
}
