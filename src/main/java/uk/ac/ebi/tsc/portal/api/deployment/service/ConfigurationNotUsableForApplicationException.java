package uk.ac.ebi.tsc.portal.api.deployment.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ConfigurationNotUsableForApplicationException extends Exception {
	
	public ConfigurationNotUsableForApplicationException(String configurationName, String applicationName) {
		super("Application " + applicationName + " is not shared in the same team/(s) as the configuration " +
				configurationName);
	}

}
