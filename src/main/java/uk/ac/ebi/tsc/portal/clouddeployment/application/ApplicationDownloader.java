package uk.ac.ebi.tsc.portal.clouddeployment.application;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vavr.Tuple2;
import io.vavr.control.Either;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.service.AccountService;
import uk.ac.ebi.tsc.portal.api.application.repo.Application;
import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationCloudProvider;
import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationCloudProviderInput;
import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationCloudProviderOutput;
import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationCloudProviderVolume;
import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationDeploymentParameter;
import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationInput;
import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationOutput;
import uk.ac.ebi.tsc.portal.api.application.repo.ApplicationVolume;
import uk.ac.ebi.tsc.portal.clouddeployment.exceptions.ApplicationDownloaderException;
import uk.ac.ebi.tsc.portal.clouddeployment.model.ApplicationManifest;
import uk.ac.ebi.tsc.portal.clouddeployment.utils.InputStreamLogger;
import uk.ac.ebi.tsc.portal.clouddeployment.utils.ManifestParser;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Component
public class ApplicationDownloader {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationDownloader.class);

	private static final String GIT_COMMAND = "git";
	private static final String RM_COMMAND = "rm";

	private AccountService accountService;
    private ProcessRunner processRunner;

    
	@Autowired
	public ApplicationDownloader(AccountService accountService, ProcessRunner processRunner) {
	    this.accountService = accountService;
		this.processRunner  = processRunner;
	}

	public Application downloadApplication(String applicationsRoot, String repoUri, String username) throws IOException, ApplicationDownloaderException {

	    logger.info(format("Downloading application from '%s' for user '%s'", repoUri, username));

		Account theAccount = this.accountService.findByUsername(username);
		
		if (theAccount != null) {
			String[] uriParts = repoUri.split("/");
			String repoName = uriParts[uriParts.length - 1];

			String path = applicationsRoot + File.separator + username.replaceAll(" ","_") + File.separator + repoName;
			File repoFolder = new File(path);
			if (repoFolder.exists() && repoFolder.isDirectory()) { // if the problem is that the folder already exists...
				logger.info("The folder '" + path + "' already exists");
				logger.info("Pulling from master instead...");
				return this.updateApplication(repoUri, path, theAccount);
			}

			logger.debug("Downloading application to " + path);

			
			//ApplicationManifest applicationManifest = ManifestParser.parseApplicationManifest(path + File.separator + "manifest.json");

			//logger.debug("Parsed manifest for application " + applicationManifest.applicationName);

			//Application application = fromManifestToApplication(repoUri, path, theAccount, applicationManifest);

			Either<Tuple2<Integer, String>, Integer> result = processRunner.run(GIT_COMMAND, "clone", "--recursive", repoUri, path);
			
			return result.map(exitStatus -> {
			    
                logger.info("Successfully downloaded application to " + path);

                String manifestPath = path + File.separator + "manifest.json";
                ApplicationManifest applicationManifest = ManifestParser.parseApplicationManifest(manifestPath);

                logger.debug("Parsed manifest for application " + applicationManifest.applicationName);

                Application application = fromManifestToApplication(repoUri, path, theAccount, applicationManifest);

                return application;
            })
	        .getOrElseThrow(left -> {
	            
			    Integer exitStatus  = left._1;
			    String  errorOutput = left._2;
			    
			    logger.error("There is an error [" + exitStatus + "] downloading from " + repoUri);
                logger.error(errorOutput);
                
                return new ApplicationDownloaderException(errorOutput);
	         })
             ;
			
		} else {
			throw new ApplicationDownloaderException(format("Cannot find account for user '%s'", username));
		}

	}

	private Application updateApplication(String repoUri, String path, Account account) throws ApplicationDownloaderException, IOException {
		logger.info("Updating application from: " + repoUri + " for user " + account.getUsername());

		ProcessBuilder processBuilder = new ProcessBuilder(GIT_COMMAND, "pull", "origin", "master");
		processBuilder.directory(new File(path));
		Process p = processBuilder.start();

		try {
			p.waitFor();
		} catch (InterruptedException e) {
			logger.error("There is an error updating from " + repoUri);
			String errorOutput = InputStreamLogger.logInputStream(p.getErrorStream());
			logger.error(errorOutput);

			throw new ApplicationDownloaderException(errorOutput);
		}

		if (p.exitValue() == 0) { // OK
			logger.info("Successfully updated application to " + path);
			logger.info(InputStreamLogger.logInputStream(p.getInputStream()));

			String manifestPath = path + File.separator + "manifest.json";
			ApplicationManifest applicationManifest = ManifestParser.parseApplicationManifest(manifestPath);

			logger.debug("Parsed manifest for application " + applicationManifest.applicationName);

			Application application = fromManifestToApplication(repoUri, path, account, applicationManifest);

			return application;
		} else if (p.exitValue() == 128) {
			logger.error("There is an error [" + p.exitValue() + "] updating from " + repoUri);
			String errorOutput = InputStreamLogger.logInputStream(p.getErrorStream());
			logger.error(errorOutput);

			throw new ApplicationDownloaderException("Repository already exists, but we shouldn't get this error...");
		} else {
			logger.error("There is an error [" + p.exitValue() + "] updating from " + repoUri);
			String errorOutput = InputStreamLogger.logInputStream(p.getErrorStream());
			logger.error(errorOutput);

			throw new ApplicationDownloaderException(errorOutput);
		}

	}

	public int removeApplication(Application application) throws IOException, ApplicationDownloaderException {

		String path = application.getRepoPath();

		logger.debug("Removing application from " + path);

		ProcessBuilder processBuilder = new ProcessBuilder(RM_COMMAND, "-r", path);
		Process p = processBuilder.start();


		try {
			p.waitFor();
		} catch (InterruptedException e) {
			logger.error("There is an error removing application from " + application.getRepoUri());
			String errorOutput = InputStreamLogger.logInputStream(p.getErrorStream());
			logger.error(errorOutput);

			throw new ApplicationDownloaderException(errorOutput);
		}

		if (p.exitValue() != 0) {
			logger.error("There is an error removing application from " + application.getRepoUri());
			String errorOutput = InputStreamLogger.logInputStream(p.getErrorStream());
			logger.error(errorOutput);

			throw new ApplicationDownloaderException(errorOutput);
		}  else {
			logger.info("Successfully removed application from " + path);
			logger.info( InputStreamLogger.logInputStream(p.getInputStream()) );
		}

		return p.exitValue();
	}

	private String generateAppReference(){
		String s[] = UUID.randomUUID().toString().split("-");
		StringBuilder reference = new StringBuilder("app-");
		reference.append(s[0], 0, 5).append("-").append(System.currentTimeMillis());
		return reference.toString();
	}

	public Application fromManifestToApplication(String repoUri, String path, Account account, ApplicationManifest applicationManifest) {

		Application application = new Application(repoUri, path, applicationManifest.applicationName, generateAppReference(), account);
		if (applicationManifest.cloudProviders != null) {
			application.getCloudProviders().addAll(
					applicationManifest.cloudProviders.stream().map(
							provider -> {
								ApplicationCloudProvider newProvider = new ApplicationCloudProvider(
										provider.cloudProvider.toString(),
										provider.path,
										application);
								if (provider.inputs!=null) {
									newProvider.getInputs().addAll(
											provider.inputs.stream().map(
													input -> new ApplicationCloudProviderInput(input, newProvider)
													).collect(Collectors.toList())
											);
								}
								if (provider.outputs != null) {
									newProvider.getOutputs().addAll(
											provider.outputs.stream().map(
													output -> new ApplicationCloudProviderOutput(output, newProvider)
													).collect(Collectors.toList())
											);
								}
								if (provider.volumes!=null) {
									newProvider.getVolumes().addAll(
											provider.volumes.stream().map(
													volume -> new ApplicationCloudProviderVolume(volume,newProvider)
													).collect(Collectors.toList())
											);
								}
								return newProvider;
							}
							).collect(Collectors.toList())
					);
		}
		if (applicationManifest.inputs != null) {
			application.getInputs().addAll(
					applicationManifest.inputs.stream().map(
								input -> {
									ApplicationInput newApplicationInput = new ApplicationInput(input.name, application);
									if(input.getValues() != null) {
										newApplicationInput.getValues().addAll(
												input.getValues());
									}
									return newApplicationInput;
								}
							).collect(Collectors.toList())
			);
		}
		if (applicationManifest.outputs != null) {
			application.getOutputs().addAll(
					applicationManifest.outputs.stream().map(param -> new ApplicationOutput(param, application)).collect(Collectors.toList())
					);
		}
		if (applicationManifest.volumes != null) {
			application.getVolumes().addAll(
					applicationManifest.volumes.stream().map(volume -> new ApplicationVolume(volume, application)).collect(Collectors.toList())
					);
		}

		if (applicationManifest.about != null) {
			application.setAbout(applicationManifest.about);
		}
		if (applicationManifest.contactEmail != null) {
			application.setContact(applicationManifest.contactEmail);
		}
		if (applicationManifest.version != null) {
			application.setVersion(applicationManifest.version);
		}
		
		//set deploymentparameters for application
		if(applicationManifest.deploymentParameters !=  null){
			application.getDeploymentParameters().addAll(
					applicationManifest.deploymentParameters.stream().
					map(param -> new ApplicationDeploymentParameter(param, application)).collect(Collectors.toList()));
		}

		return application;
	}


}
