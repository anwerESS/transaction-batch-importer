package com.example.transactionbatchimporter.batch.tasklet;

import com.example.transactionbatchimporter.batch.file.TransactionInputFileResolver;
import com.example.transactionbatchimporter.exception.InvalidInputFileException;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
public class ValidateInputFileTasklet implements Tasklet {

	private static final String EXPECTED_HEADER =
			"transactionId,accountNumber,amount,currency,type,transactionDate";

	private final TransactionInputFileResolver inputFileResolver;
	private final String fileName;

	public ValidateInputFileTasklet(
			TransactionInputFileResolver inputFileResolver,
			@Value("#{jobParameters['fileName']}") String fileName) {
		this.inputFileResolver = inputFileResolver;
		this.fileName = fileName;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		Path inputFile = inputFileResolver.resolveInputFile(fileName);
		log.info("Validating transaction input file {}.", inputFile.toAbsolutePath());

		validateCsvExtension(inputFile);
		validateFileExists(inputFile);
		validateFileIsNotEmpty(inputFile);
		validateHeader(inputFile);

		// ArchiveInputFileTasklet reuses the exact file validated at the start of the job.
		chunkContext.getStepContext()
				.getStepExecution()
				.getJobExecution()
				.getExecutionContext()
				.putString(TransactionImportExecutionContextKeys.INPUT_FILE_PATH, inputFile.toString());

		log.info("Transaction input file {} is valid.", inputFile.toAbsolutePath());
		return RepeatStatus.FINISHED;
	}

	private void validateCsvExtension(Path inputFile) {
		String lowerCaseFileName = inputFile.getFileName().toString().toLowerCase(Locale.ROOT);
		if (!lowerCaseFileName.endsWith(".csv")) {
			throw new InvalidInputFileException("Input file must have a .csv extension.");
		}
	}

	private void validateFileExists(Path inputFile) {
		if (!Files.isRegularFile(inputFile)) {
			throw new InvalidInputFileException("Input CSV file does not exist: " + inputFile.toAbsolutePath() + ".");
		}
	}

	private void validateFileIsNotEmpty(Path inputFile) {
		try {
			if (Files.size(inputFile) == 0) {
				throw new InvalidInputFileException("Input CSV file is empty: " + inputFile.toAbsolutePath() + ".");
			}
		} catch (IOException ex) {
			throw new InvalidInputFileException("Unable to read input CSV file size: "
					+ inputFile.toAbsolutePath() + ".", ex);
		}
	}

	private void validateHeader(Path inputFile) {
		try (BufferedReader reader = Files.newBufferedReader(inputFile, StandardCharsets.UTF_8)) {
			String header = reader.readLine();
			if (!EXPECTED_HEADER.equals(removeUtf8Bom(header).trim())) {
				throw new InvalidInputFileException("Input CSV header must be: " + EXPECTED_HEADER + ".");
			}
		} catch (IOException ex) {
			throw new InvalidInputFileException("Unable to read input CSV header: "
					+ inputFile.toAbsolutePath() + ".", ex);
		}
	}

	private String removeUtf8Bom(String value) {
		if (value == null) {
			return "";
		}
		return value.startsWith("\uFEFF") ? value.substring(1) : value;
	}
}
