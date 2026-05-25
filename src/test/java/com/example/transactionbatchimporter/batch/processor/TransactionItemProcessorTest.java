package com.example.transactionbatchimporter.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.transactionbatchimporter.dto.TransactionCsvDto;
import com.example.transactionbatchimporter.entity.TransactionEntity;
import com.example.transactionbatchimporter.enums.CurrencyCode;
import com.example.transactionbatchimporter.enums.TransactionType;
import com.example.transactionbatchimporter.exception.InvalidTransactionException;
import com.example.transactionbatchimporter.repository.TransactionRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionItemProcessorTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
	private TransactionRepository transactionRepository;
	private TransactionItemProcessor processor;

	@BeforeEach
	void setUp() {
		transactionRepository = mock(TransactionRepository.class);
		processor = new TransactionItemProcessor(validator, transactionRepository);
	}

	@Test
	void validCreditTransactionShouldBeConvertedToEntity() {
		TransactionCsvDto dto = validTransaction()
				.type("CREDIT")
				.amount(new BigDecimal("120.50"))
				.build();

		TransactionEntity entity = processor.process(dto);

		assertThat(entity.getTransactionId()).isEqualTo("TXN-001");
		assertThat(entity.getAccountNumber()).isEqualTo("ACC-1001");
		assertThat(entity.getAmount()).isEqualByComparingTo("120.50");
		assertThat(entity.getCurrency()).isEqualTo(CurrencyCode.EUR);
		assertThat(entity.getType()).isEqualTo(TransactionType.CREDIT);
		assertThat(entity.getTransactionDate()).isEqualTo(LocalDate.of(2026, 5, 20));
		assertThat(entity.getImportedAt()).isNotNull();
	}

	@Test
	void validDebitTransactionShouldBeStoredWithNegativeAmount() {
		TransactionCsvDto dto = validTransaction()
				.type("DEBIT")
				.amount(new BigDecimal("45.00"))
				.build();

		TransactionEntity entity = processor.process(dto);

		assertThat(entity.getAmount()).isEqualByComparingTo("-45.00");
		assertThat(entity.getType()).isEqualTo(TransactionType.DEBIT);
	}

	@Test
	void missingTransactionIdShouldThrowInvalidTransactionException() {
		TransactionCsvDto dto = validTransaction()
				.transactionId(" ")
				.build();

		assertThatThrownBy(() -> processor.process(dto))
				.isInstanceOf(InvalidTransactionException.class)
				.hasMessageContaining("transactionId is mandatory");
	}

	@Test
	void invalidCurrencyShouldThrowInvalidTransactionException() {
		TransactionCsvDto dto = validTransaction()
				.currency("GBP")
				.build();

		assertThatThrownBy(() -> processor.process(dto))
				.isInstanceOf(InvalidTransactionException.class)
				.hasMessageContaining("currency must be EUR or USD");
	}

	@Test
	void invalidTypeShouldThrowInvalidTransactionException() {
		TransactionCsvDto dto = validTransaction()
				.type("TRANSFER")
				.build();

		assertThatThrownBy(() -> processor.process(dto))
				.isInstanceOf(InvalidTransactionException.class)
				.hasMessageContaining("type must be CREDIT or DEBIT");
	}

	@Test
	void zeroAmountShouldThrowInvalidTransactionException() {
		TransactionCsvDto dto = validTransaction()
				.amount(BigDecimal.ZERO)
				.build();

		assertThatThrownBy(() -> processor.process(dto))
				.isInstanceOf(InvalidTransactionException.class)
				.hasMessageContaining("amount must be greater than zero");
	}

	@Test
	void missingTransactionDateShouldThrowInvalidTransactionException() {
		TransactionCsvDto dto = validTransaction()
				.transactionDate(null)
				.build();

		assertThatThrownBy(() -> processor.process(dto))
				.isInstanceOf(InvalidTransactionException.class)
				.hasMessageContaining("transactionDate is mandatory");
	}

	@Test
	void existingTransactionIdShouldBeFiltered() {
		TransactionCsvDto dto = validTransaction()
				.transactionId("TXN-001")
				.build();
		when(transactionRepository.existsByTransactionId("TXN-001")).thenReturn(true);

		TransactionEntity entity = processor.process(dto);

		assertThat(entity).isNull();
	}

	@Test
	void duplicatedTransactionIdInSameStepShouldBeFiltered() {
		TransactionCsvDto dto = validTransaction()
				.transactionId("TXN-001")
				.build();

		TransactionEntity firstEntity = processor.process(dto);
		TransactionEntity secondEntity = processor.process(dto);

		assertThat(firstEntity).isNotNull();
		assertThat(secondEntity).isNull();
	}

	private TransactionCsvDto.TransactionCsvDtoBuilder validTransaction() {
		return TransactionCsvDto.builder()
				.transactionId("TXN-001")
				.accountNumber("ACC-1001")
				.amount(new BigDecimal("120.50"))
				.currency("EUR")
				.type("CREDIT")
				.transactionDate(LocalDate.of(2026, 5, 20));
	}
}
