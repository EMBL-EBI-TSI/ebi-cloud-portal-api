package uk.ac.ebi.tsc.portal.api.configuration.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class CloudProviderParametersNotFoundException extends RuntimeException {

    public CloudProviderParametersNotFoundException(String username, String name, String cloudCredentialsName) {
        super("Could not create configuration '" + name + "' for user '" + username + "'. Cannot find cloud provider parameters '" + cloudCredentialsName +"'.");
    }

}