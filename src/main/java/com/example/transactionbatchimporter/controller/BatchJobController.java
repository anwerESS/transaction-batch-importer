package com.example.transactionbatchimporter.controller;

import com.example.transactionbatchimporter.dto.BatchJobResponse;
import com.example.transactionbatchimporter.exception.BatchJobNotFoundException;
import com.example.transactionbatchimporter.exception.BatchImportException;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class BatchJobController {

	private static final String DEFAULT_FILE_NAME = "transactions.csv";

	private final JobLauncher jobLauncher;
	private final JobExplorer jobExplorer;
	private final Job importTransactionsJob;

	@PostMapping("/api/batch/transactions/import")
	public ResponseEntity<BatchJobResponse> importTransactions() {
		try {
			JobParameters jobParameters = new JobParametersBuilder()
					.addString("fileName", DEFAULT_FILE_NAME)
					.addLong("startedAt", System.currentTimeMillis())
					.addString("requestId", UUID.randomUUID().toString())
					.toJobParameters();

			// Le JobLauncher asynchrone cree l'execution Spring Batch puis rend la main au thread HTTP.
			JobExecution jobExecution = jobLauncher.run(importTransactionsJob, jobParameters);
			URI statusUri = ServletUriComponentsBuilder.fromCurrentContextPath()
					.path("/api/batch/jobs/{jobExecutionId}")
					.buildAndExpand(jobExecution.getId())
					.toUri();

			return ResponseEntity
					.status(HttpStatus.ACCEPTED)
					.location(statusUri)
					.body(toResponse(jobExecution));
		} catch (BatchImportException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new BatchImportException("Unable to start transaction import job.", ex);
		}
	}

	@GetMapping("/api/batch/jobs/{jobExecutionId}")
	public BatchJobResponse getJobExecution(@PathVariable long jobExecutionId) {
		JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);
		if (jobExecution == null) {
			throw new BatchJobNotFoundException(jobExecutionId);
		}

		return toResponse(jobExecution);
	}

	private BatchJobResponse toResponse(JobExecution jobExecution) {
		ExitStatus exitStatus = jobExecution.getExitStatus();
		return BatchJobResponse.builder()
				.jobExecutionId(jobExecution.getId())
				.jobName(jobExecution.getJobInstance().getJobName())
				.status(jobExecution.getStatus().name())
				.createTime(jobExecution.getCreateTime())
				.startTime(jobExecution.getStartTime())
				.endTime(jobExecution.getEndTime())
				.exitCode(exitStatus == null ? null : exitStatus.getExitCode())
				.exitDescription(exitStatus == null ? null : exitStatus.getExitDescription())
				.build();
	}
}
