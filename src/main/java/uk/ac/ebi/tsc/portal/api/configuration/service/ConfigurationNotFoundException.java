package uk.ac.ebi.tsc.portal.api.configuration.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ConfigurationNotFoundException extends RuntimeException {

    public ConfigurationNotFoundException(String userId) {
        super("Could not find user '" + userId + "'.");
    }

    public ConfigurationNotFoundException(String username, String name) {
        super("Could not find configuration " + name + " with owner reference '" + username + "'.");
    }

    public ConfigurationNotFoundException(Long cloudCredentialsId) {
        super("Could not configuration for ID '" + cloudCredentialsId + "'.");
    }

}