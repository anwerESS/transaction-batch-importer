package com.example.transactionbatchimporter.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidInputFileException extends RuntimeException {

	public InvalidInputFileException(String message) {
		super(message);
	}

	public InvalidInputFileException(String message, Throwable cause) {
		super(message, cause);
	}
}
