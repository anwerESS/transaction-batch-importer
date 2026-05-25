package com.example.transactionbatchimporter.exception;

public class ImportReportException extends BatchImportException {

	public ImportReportException(String message) {
		super(message);
	}

	public ImportReportException(String message, Throwable cause) {
		super(message, cause);
	}
}
