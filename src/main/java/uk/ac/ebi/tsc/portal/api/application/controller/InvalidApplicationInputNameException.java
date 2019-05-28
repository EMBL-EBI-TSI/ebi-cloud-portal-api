package uk.ac.ebi.tsc.portal.api.application.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidApplicationInputNameException extends RuntimeException{

	public InvalidApplicationInputNameException(String inputName) {
		super("Application input '" + inputName + "' does not exist '");
	}

}
