package com.example.transactionbatchimporter.batch.tasklet;

import com.example.transactionbatchimporter.batch.file.TransactionInputFileResolver;
import com.example.transactionbatchimporter.exception.InputFileArchiveException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveInputFileTasklet implements Tasklet {

	private final TransactionInputFileResolver inputFileResolver;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		JobExecution jobExecution = chunkContext.getStepContext().getStepExecution().getJobExecution();
		Path inputFile = getValidatedInputFile(jobExecution.getExecutionContext());
		Path archiveFile = inputFileResolver.resolveArchiveFile(inputFile, jobExecution.getId());

		try {
			Files.createDirectories(archiveFile.getParent());
			Files.move(inputFile, archiveFile);
			log.info("Archived transaction input file from {} to {}.",
					inputFile.toAbsolutePath(),
					archiveFile.toAbsolutePath());
			return RepeatStatus.FINISHED;
		} catch (IOException ex) {
			throw new InputFileArchiveException("Unable to archive input CSV file from "
					+ inputFile.toAbsolutePath() + " to " + archiveFile.toAbsolutePath() + ".", ex);
		}
	}

	private Path getValidatedInputFile(ExecutionContext executionContext) {
		if (!executionContext.containsKey(TransactionImportExecutionContextKeys.INPUT_FILE_PATH)) {
			throw new InputFileArchiveException("Validated input file path is missing from the job execution context.");
		}

		return Path.of(executionContext.getString(TransactionImportExecutionContextKeys.INPUT_FILE_PATH));
	}
}
