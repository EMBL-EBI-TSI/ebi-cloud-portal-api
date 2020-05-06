package uk.ac.ebi.tsc.portal.api.deployment.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class DeploymentConfigurationNotFoundException extends RuntimeException {

	public DeploymentConfigurationNotFoundException(String configurationName) {
		super("Could not find deployment configuration with reference " + configurationName);
	}

	public DeploymentConfigurationNotFoundException() {
		super("Could not find deployment configuration for the user");
	}

}
