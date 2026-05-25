package com.example.transactionbatchimporter.exception;

public class InputFileArchiveException extends BatchImportException {

	public InputFileArchiveException(String message) {
		super(message);
	}

	public InputFileArchiveException(String message, Throwable cause) {
		super(message, cause);
	}
}
