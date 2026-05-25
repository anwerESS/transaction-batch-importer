package com.example.transactionbatchimporter.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class BatchImportException extends RuntimeException {

	public BatchImportException(String message) {
		super(message);
	}

	public BatchImportException(String message, Throwable cause) {
		super(message, cause);
	}
}
