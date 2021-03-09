package uk.ac.ebi.tsc.portal.api.team.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TeamAccessDeniedException extends RuntimeException {

    public TeamAccessDeniedException(String teamName) {
        super("Only owner of the team " + teamName + " can access contact emails");
    }
}
