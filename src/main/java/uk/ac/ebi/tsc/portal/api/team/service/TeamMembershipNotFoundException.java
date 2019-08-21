package uk.ac.ebi.tsc.portal.api.team.service;

/**
 * @author Felix Xavier <famaladoss@ebi.ac.uk>
 * @since v0.0.1
 */
public class TeamMembershipNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 134243L;

    public TeamMembershipNotFoundException(String teamName) {
        super("User is not member of the team [ " + teamName + "], Please request to join with team.");
    }
}
