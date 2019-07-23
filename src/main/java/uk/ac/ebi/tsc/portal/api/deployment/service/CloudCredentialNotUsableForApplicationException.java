package uk.ac.ebi.tsc.portal.api.deployment.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class CloudCredentialNotUsableForApplicationException extends Exception {
	
	public CloudCredentialNotUsableForApplicationException(String credentialName, String applicationName) {
		super("Application " + applicationName + " is not shared in the same team/(s) as the cloud credential " +
				credentialName);
	}

}
