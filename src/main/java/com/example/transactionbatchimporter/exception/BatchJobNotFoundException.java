package com.example.transactionbatchimporter.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BatchJobNotFoundException extends RuntimeException {

	private final long jobExecutionId;

	public BatchJobNotFoundException(long jobExecutionId) {
		super("Batch job execution " + jobExecutionId + " was not found.");
		this.jobExecutionId = jobExecutionId;
	}

	public long getJobExecutionId() {
		return jobExecutionId;
	}
}
