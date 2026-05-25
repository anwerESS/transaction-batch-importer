package com.example.transactionbatchimporter.batch.tasklet;

import com.example.transactionbatchimporter.batch.file.TransactionInputFileResolver;
import com.example.transactionbatchimporter.exception.ImportReportException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateImportReportTasklet implements Tasklet {

	private static final String IMPORT_STEP_NAME = "importTransactionsStep";

	private final TransactionInputFileResolver inputFileResolver;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		JobExecution jobExecution = chunkContext.getStepContext().getStepExecution().getJobExecution();
		StepExecution importStepExecution = findImportStepExecution(jobExecution);
		Path reportFile = inputFileResolver.resolveReportFile(jobExecution.getId());

		try {
			Files.createDirectories(reportFile.getParent());
			Files.writeString(reportFile, buildReport(jobExecution, importStepExecution), StandardCharsets.UTF_8);
			log.info("Generated transaction import report {}.", reportFile.toAbsolutePath());
			return RepeatStatus.FINISHED;
		} catch (IOException ex) {
			throw new ImportReportException("Unable to generate import report: "
					+ reportFile.toAbsolutePath() + ".", ex);
		}
	}

	private StepExecution findImportStepExecution(JobExecution jobExecution) {
		return jobExecution.getStepExecutions().stream()
				.filter(stepExecution -> IMPORT_STEP_NAME.equals(stepExecution.getStepName()))
				.findFirst()
				.orElseThrow(() -> new ImportReportException("Unable to find import step execution."));
	}

	private String buildReport(JobExecution jobExecution, StepExecution importStepExecution) {
		return """
				Transaction Import Report
				jobExecutionId=%s
				jobName=%s
				status=%s
				startTime=%s
				endTime=%s
				readCount=%d
				writeCount=%d
				skipCount=%d
				rollbackCount=%d
				"""
				.formatted(
						jobExecution.getId(),
						jobExecution.getJobInstance().getJobName(),
						importStepExecution.getStatus(),
						formatDateTime(importStepExecution.getStartTime()),
						formatDateTime(importStepExecution.getEndTime()),
						importStepExecution.getReadCount(),
						importStepExecution.getWriteCount(),
						importStepExecution.getSkipCount(),
						importStepExecution.getRollbackCount());
	}

	private String formatDateTime(LocalDateTime value) {
		return value == null ? "N/A" : value.toString();
	}
}
