package com.example.transactionbatchimporter.batch.processor;

import com.example.transactionbatchimporter.dto.TransactionCsvDto;
import com.example.transactionbatchimporter.entity.TransactionEntity;
import com.example.transactionbatchimporter.enums.CurrencyCode;
import com.example.transactionbatchimporter.enums.TransactionType;
import com.example.transactionbatchimporter.exception.InvalidTransactionException;
import com.example.transactionbatchimporter.repository.TransactionRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class TransactionItemProcessor implements ItemProcessor<TransactionCsvDto, TransactionEntity> {

	private final Validator validator;
	private final TransactionRepository transactionRepository;
	private final Set<String> transactionIdsSeenInCurrentStep = new HashSet<>();

	@Override
	public TransactionEntity process(TransactionCsvDto item) {
		validateCsvRecord(item);

		String transactionId = item.getTransactionId().trim();
		if (!transactionIdsSeenInCurrentStep.add(transactionId)) {
			// Evite aussi les doublons presents deux fois dans le meme fichier CSV.
			return null;
		}

		if (transactionRepository.existsByTransactionId(transactionId)) {
			// Le job doit etre idempotent : relancer le meme fichier ne doit pas recreer les memes transactions.
			return null;
		}

		CurrencyCode currency = parseCurrency(item.getCurrency(), item.getTransactionId());
		TransactionType type = parseType(item.getType(), item.getTransactionId());
		BigDecimal normalizedAmount = normalizeAmount(item.getAmount(), type, item.getTransactionId());

		return TransactionEntity.builder()
				.transactionId(transactionId)
				.accountNumber(item.getAccountNumber().trim())
				.amount(normalizedAmount)
				.currency(currency)
				.type(type)
				.transactionDate(item.getTransactionDate())
				.importedAt(LocalDateTime.now())
				.build();
	}

	private void validateCsvRecord(TransactionCsvDto item) {
		if (item == null) {
			throw new InvalidTransactionException("Transaction record is mandatory.");
		}

		Set<ConstraintViolation<TransactionCsvDto>> violations = validator.validate(item);
		if (!violations.isEmpty()) {
			String transactionReference = item.getTransactionId() == null || item.getTransactionId().isBlank()
					? "unknown"
					: item.getTransactionId();
			String message = violations.stream()
					.sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
					.map(ConstraintViolation::getMessage)
					.collect(Collectors.joining("; "));
			throw new InvalidTransactionException("Invalid transaction " + transactionReference + ": " + message + ".");
		}
	}

	private CurrencyCode parseCurrency(String currency, String transactionId) {
		try {
			return CurrencyCode.valueOf(currency.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new InvalidTransactionException("currency must be EUR or USD for transaction "
					+ transactionId + ".");
		}
	}

	private TransactionType parseType(String type, String transactionId) {
		try {
			return TransactionType.valueOf(type.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new InvalidTransactionException("type must be CREDIT or DEBIT for transaction "
					+ transactionId + ".");
		}
	}

	private BigDecimal normalizeAmount(BigDecimal amount, TransactionType type, String transactionId) {
		if (type == TransactionType.CREDIT) {
			return amount;
		}

		// Fonctionnellement, un DEBIT est stocke comme une sortie d'argent, donc en montant negatif.
		return amount.abs().negate();
	}
}
