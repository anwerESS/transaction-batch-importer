package com.example.transactionbatchimporter.batch.file;

import com.example.transactionbatchimporter.exception.InvalidInputFileException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class TransactionInputFileResolver {

	private static final String DEFAULT_FILE_NAME = "transactions.csv";
	private static final DateTimeFormatter ARCHIVE_TIMESTAMP_FORMATTER =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	private final TransactionImportFileProperties properties;

	public Path resolveInputFile(String fileName) {
		return properties.getInputDirectory()
				.resolve(normalizeFileName(fileName))
				.normalize();
	}

	public Path resolveArchiveFile(Path inputFile, Long jobExecutionId) {
		return properties.getArchiveDirectory()
				.resolve(buildArchiveFileName(inputFile.getFileName().toString(), jobExecutionId))
				.normalize();
	}

	public Path resolveReportFile(Long jobExecutionId) {
		return properties.getReportDirectory()
				.resolve("transaction-import-report-" + jobExecutionId + ".txt")
				.normalize();
	}

	private String normalizeFileName(String fileName) {
		String normalizedFileName = StringUtils.hasText(fileName) ? fileName.trim() : DEFAULT_FILE_NAME;
		Path fileNamePath = Path.of(normalizedFileName);

		if (fileNamePath.isAbsolute() || fileNamePath.getNameCount() != 1 || normalizedFileName.contains("..")) {
			throw new InvalidInputFileException("Input file name must be a simple CSV file name.");
		}

		return normalizedFileName;
	}

	private String buildArchiveFileName(String fileName, Long jobExecutionId) {
		String baseName = fileName;
		String extension = "";
		int extensionIndex = fileName.lastIndexOf('.');
		if (extensionIndex > 0) {
			baseName = fileName.substring(0, extensionIndex);
			extension = fileName.substring(extensionIndex).toLowerCase(Locale.ROOT);
		}

		String timestamp = LocalDateTime.now().format(ARCHIVE_TIMESTAMP_FORMATTER);
		return baseName + "-job-" + jobExecutionId + "-" + timestamp + extension;
	}
}
