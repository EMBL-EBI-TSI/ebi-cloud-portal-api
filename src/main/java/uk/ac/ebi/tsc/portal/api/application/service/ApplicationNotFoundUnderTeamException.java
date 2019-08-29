package uk.ac.ebi.tsc.portal.api.application.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


/**
 * @author Felix Xavier <famaladoss@ebi.ac.uk>
 * @since v0.0.1
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ApplicationNotFoundUnderTeamException extends RuntimeException {

	public ApplicationNotFoundUnderTeamException(String applicationName, String team) {
        super("Application '" + applicationName + "' has not been shared within team  \'" + team + "\'.");
    }

}