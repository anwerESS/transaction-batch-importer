package com.example.transactionbatchimporter.batch.reader;

import com.example.transactionbatchimporter.dto.TransactionCsvDto;
import com.example.transactionbatchimporter.exception.InvalidTransactionException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class TransactionCsvReaderConfig {

	private static final String DEFAULT_FILE_NAME = "transactions.csv";
	private static final Path DEV_INPUT_DIRECTORY = Path.of("src/main/resources/input");

	@Bean
	@StepScope
	public FlatFileItemReader<TransactionCsvDto> transactionCsvItemReader(
			@Value("#{jobParameters['fileName']}") String fileName) {
		String sourceFile = fileName == null || fileName.isBlank() ? DEFAULT_FILE_NAME : fileName;
		Resource inputResource = resolveInputResource(sourceFile);

		return new FlatFileItemReaderBuilder<TransactionCsvDto>()
				.name("transactionCsvItemReader")
				.resource(inputResource)
				.linesToSkip(1)
				.delimited()
				.names("transactionId", "accountNumber", "amount", "currency", "type", "transactionDate")
				.fieldSetMapper(transactionFieldSetMapper())
				.build();
	}

	private Resource resolveInputResource(String sourceFile) {
		Path devInputFile = DEV_INPUT_DIRECTORY.resolve(sourceFile);
		if (Files.isRegularFile(devInputFile)) {
			log.info("Reading transaction CSV from filesystem: {}.", devInputFile.toAbsolutePath());
			return new FileSystemResource(devInputFile);
		}

		log.info("Reading transaction CSV from classpath: input/{}.", sourceFile);
		return new ClassPathResource("input/" + sourceFile);
	}

	private FieldSetMapper<TransactionCsvDto> transactionFieldSetMapper() {
		return fieldSet -> TransactionCsvDto.builder()
				.transactionId(trim(fieldSet.readString("transactionId")))
				.accountNumber(trim(fieldSet.readString("accountNumber")))
				.amount(parseAmount(fieldSet.readString("amount"), fieldSet.readString("transactionId")))
				.currency(trim(fieldSet.readString("currency")))
				.type(trim(fieldSet.readString("type")))
				.transactionDate(parseTransactionDate(
						fieldSet.readString("transactionDate"),
						fieldSet.readString("transactionId")))
				.build();
	}

	private String trim(String value) {
		return value == null ? null : value.trim();
	}

	private BigDecimal parseAmount(String amount, String transactionId) {
		if (!StringUtils.hasText(amount)) {
			return null;
		}
		try {
			return new BigDecimal(amount.trim());
		} catch (NumberFormatException ex) {
			throw new InvalidTransactionException("amount must be a valid decimal number for transaction "
					+ transactionReference(transactionId) + ".");
		}
	}

	private LocalDate parseTransactionDate(String transactionDate, String transactionId) {
		if (!StringUtils.hasText(transactionDate)) {
			return null;
		}
		try {
			return LocalDate.parse(transactionDate.trim());
		} catch (DateTimeParseException ex) {
			throw new InvalidTransactionException("transactionDate must use ISO format yyyy-MM-dd for transaction "
					+ transactionReference(transactionId) + ".");
		}
	}

	private String transactionReference(String transactionId) {
		return StringUtils.hasText(transactionId) ? transactionId.trim() : "unknown";
	}
}
