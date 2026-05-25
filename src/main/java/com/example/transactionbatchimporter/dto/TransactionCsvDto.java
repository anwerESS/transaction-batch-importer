package com.example.transactionbatchimporter.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCsvDto {

	// Identifiant fonctionnel fourni par le fichier CSV.
	@NotBlank(message = "transactionId is mandatory")
	private String transactionId;

	// Compte client concerne par la transaction.
	@NotBlank(message = "accountNumber is mandatory")
	private String accountNumber;

	// Montant lu tel quel dans le CSV ; le processor applique ensuite la regle CREDIT/DEBIT.
	@NotNull(message = "amount is mandatory")
	@Positive(message = "amount must be greater than zero")
	@Digits(integer = 17, fraction = 2, message = "amount must have at most 17 integer digits and 2 decimal digits")
	private BigDecimal amount;

	// Devise sous forme texte pour laisser le processor valider et convertir vers CurrencyCode.
	@NotBlank(message = "currency is mandatory")
	@Pattern(regexp = "(?i)EUR|USD", message = "currency must be EUR or USD")
	private String currency;

	// Type sous forme texte pour valider explicitement CREDIT ou DEBIT.
	@NotBlank(message = "type is mandatory")
	@Pattern(regexp = "(?i)CREDIT|DEBIT", message = "type must be CREDIT or DEBIT")
	private String type;

	// Date metier de la transaction, differente de la date d'import.
	@NotNull(message = "transactionDate is mandatory")
	private LocalDate transactionDate;
}
