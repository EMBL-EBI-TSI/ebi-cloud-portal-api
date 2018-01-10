package uk.ac.ebi.tsc.portal.api.team.controller;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
public class TeamNotDeletedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TeamNotDeletedException(String teamName){
		
		super("Team " + teamName + " failed to get deleted");
		
	}
}
