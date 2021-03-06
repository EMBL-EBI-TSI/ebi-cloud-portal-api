package uk.ac.ebi.tsc.portal.api.cloudproviderparameters.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class CloudProviderParametersNotSharedException extends RuntimeException {

	public CloudProviderParametersNotSharedException(String givenName, String cppName) {
        super("CloudProviderParameters '" + cppName + "' has not been shared with user \'" + givenName + "\'.");
    }

}