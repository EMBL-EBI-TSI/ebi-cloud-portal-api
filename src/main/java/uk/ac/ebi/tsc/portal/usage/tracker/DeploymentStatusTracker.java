package uk.ac.ebi.tsc.portal.usage.tracker;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopyRepository;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParametersService;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service.CloudProviderParamsCopyService;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationDeploymentParametersService;
import uk.ac.ebi.tsc.portal.api.configuration.service.ConfigurationService;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentConfigurationService;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.encryptdecrypt.security.EncryptionService;
import uk.ac.ebi.tsc.portal.api.utils.SendMail;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDeployer;
import uk.ac.ebi.tsc.portal.usage.deployment.service.DeploymentIndexService;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Component
public class DeploymentStatusTracker {

	private static final Logger logger = LoggerFactory.getLogger(DeploymentStatusTracker.class);

	@Value("${be.deployments.root}")
	private String deploymentsRoot;

	@Value("${elasticsearch.url}")
	private String elasticSearchUrl;

	@Value("${elasticsearch.index}")
	private String elasticSearchIndex;

	@Value("${elasticsearch.username}")
	private String elasticSearchUsername;

	@Value("${elasticsearch.password}")
	private String elasticSearchPassword;

	private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

	private final DeploymentService deploymentService;
	private final ConfigurationService configurationService;
	private final DeploymentConfigurationService deploymentConfigurationService;
	private final CloudProviderParametersService cloudProviderParametersService;
	private final ConfigurationDeploymentParametersService configurationDeploymentParametersService;
	private final ApplicationDeployer applicationDeployer;
	private final CloudProviderParamsCopyService cloudProviderParametersCopyService;
	private final CloudProviderParamsCopyRepository cloudProviderParametersCopyRepository;
	private final EncryptionService encryptionService;
	private final SendMail sendMail;

	@Autowired
	public DeploymentStatusTracker(DeploymentService deploymentService,
                                   CloudProviderParamsCopyService cloudProviderParametersCopyService,
                                   ConfigurationService configurationService,
                                   DomainService domainService,
                                   CloudProviderParametersService cloudProviderParametersService,
                                   ConfigurationDeploymentParametersService configurationDeploymentParametersService,
                                   DeploymentConfigurationService deploymentConfigurationService,
                                   ApplicationDeployer applicationDeployer,
                                   EncryptionService encryptionService,
                                   CloudProviderParamsCopyRepository cloudProviderParametersCopyRepository,
                                   SendMail sendMail) {
		this.deploymentService = deploymentService;
		this.cloudProviderParametersCopyService = cloudProviderParametersCopyService;
		this.cloudProviderParametersService = cloudProviderParametersService;
		this.configurationDeploymentParametersService = configurationDeploymentParametersService;
		this.configurationService = configurationService;
		this.deploymentConfigurationService = deploymentConfigurationService;
		this.applicationDeployer = applicationDeployer;
		scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(4);
		this.encryptionService = encryptionService;
		this.cloudProviderParametersCopyRepository = cloudProviderParametersCopyRepository;
		this.sendMail = sendMail;
	}

	public void start(long initialDelay, long periodInSeconds) {
		logger.info("Starting Deployment Status Tracker... (1/4)");
		DeploymentIndexService deploymentIndexService = new DeploymentIndexService(
				new RestTemplate(),
				this.elasticSearchUrl + "/" + this.elasticSearchIndex,
				this.elasticSearchUsername,
				this.elasticSearchPassword);
		logger.info("DeploymentIndexService initiated... (2/4)");
		scheduledThreadPoolExecutor.scheduleAtFixedRate(
				new DeploymentStatusUpdate(deploymentIndexService, deploymentService, 
						cloudProviderParametersCopyRepository, encryptionService, cloudProviderParametersCopyService),
				initialDelay,
				periodInSeconds,
				TimeUnit.SECONDS
				);
		logger.info("DeploymentStatusUpdate thread scheduled... (3/4)");
		scheduledThreadPoolExecutor.scheduleAtFixedRate(
				new ConfigurationUsageMonitor(
						deploymentIndexService,
						deploymentService,
						configurationService,
						cloudProviderParametersCopyService,
						deploymentConfigurationService,
                        applicationDeployer,
                        sendMail),
				initialDelay,
				periodInSeconds,
				TimeUnit.SECONDS
				);
		logger.info("ConfigurationUsageMonitor thread scheduled... (4/4)");
	}
}
