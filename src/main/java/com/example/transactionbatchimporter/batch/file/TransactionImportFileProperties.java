package com.example.transactionbatchimporter.batch.file;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TransactionImportFileProperties {

	private final Path inputDirectory;
	private final Path archiveDirectory;
	private final Path reportDirectory;

	public TransactionImportFileProperties(
			@Value("${app.transaction-import.input-directory:src/main/resources/input}") String inputDirectory,
			@Value("${app.transaction-import.archive-directory:target/archive}") String archiveDirectory,
			@Value("${app.transaction-import.report-directory:target/import-reports}") String reportDirectory) {
		this.inputDirectory = Path.of(inputDirectory);
		this.archiveDirectory = Path.of(archiveDirectory);
		this.reportDirectory = Path.of(reportDirectory);
	}

	public Path getInputDirectory() {
		return inputDirectory;
	}

	public Path getArchiveDirectory() {
		return archiveDirectory;
	}

	public Path getReportDirectory() {
		return reportDirectory;
	}
}
