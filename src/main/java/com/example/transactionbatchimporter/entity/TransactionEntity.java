package com.example.transactionbatchimporter.entity;

import com.example.transactionbatchimporter.enums.CurrencyCode;
import com.example.transactionbatchimporter.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "transactions",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_transactions_transaction_id",
				columnNames = "transaction_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Identifiant metier conserve pour detecter les doublons et tracer la ligne importee.
	@Column(nullable = false)
	private String transactionId;

	@Column(nullable = false)
	private String accountNumber;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CurrencyCode currency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TransactionType type;

	@Column(nullable = false)
	private LocalDate transactionDate;

	// Timestamp technique ajoute au moment du traitement batch.
	@Column(nullable = false)
	private LocalDateTime importedAt;
}
