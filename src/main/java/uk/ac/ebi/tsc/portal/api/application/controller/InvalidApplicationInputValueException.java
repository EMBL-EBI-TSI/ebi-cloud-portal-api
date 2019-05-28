package uk.ac.ebi.tsc.portal.api.application.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidApplicationInputValueException extends RuntimeException {

		public InvalidApplicationInputValueException(String  inputName, String inputValue) {
			super("Application input value '" + inputValue + "' is not valid for input '" + inputName + "'");
		}

	}
