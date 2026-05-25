package com.example.transactionbatchimporter.batch.reader;

import com.example.transactionbatchimporter.batch.file.TransactionInputFileResolver;
import com.example.transactionbatchimporter.dto.TransactionCsvDto;
import com.example.transactionbatchimporter.exception.InvalidTransactionException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TransactionCsvReaderConfig {

	private final TransactionInputFileResolver inputFileResolver;

	@Bean
	@StepScope
	public FlatFileItemReader<TransactionCsvDto> transactionCsvItemReader(
			@Value("#{jobParameters['fileName']}") String fileName) {
		Path inputFile = inputFileResolver.resolveInputFile(fileName);
		log.info("Reading transaction CSV from filesystem: {}.", inputFile.toAbsolutePath());

		return new FlatFileItemReaderBuilder<TransactionCsvDto>()
				.name("transactionCsvItemReader")
				.resource(new FileSystemResource(inputFile))
				.linesToSkip(1)
				.delimited()
				.names("transactionId", "accountNumber", "amount", "currency", "type", "transactionDate")
				.fieldSetMapper(transactionFieldSetMapper())
				.build();
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
