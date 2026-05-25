package com.example.transactionbatchimporter.repository;

import com.example.transactionbatchimporter.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

	boolean existsByTransactionId(String transactionId);
}
