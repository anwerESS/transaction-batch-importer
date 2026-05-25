package com.example.transactionbatchimporter.batch.writer;

import com.example.transactionbatchimporter.entity.TransactionEntity;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionItemWriterConfig {

	@Bean
	public JpaItemWriter<TransactionEntity> transactionJpaItemWriter(EntityManagerFactory entityManagerFactory) {
		// Le writer delegue la persistance a JPA et participe a la transaction du chunk.
		return new JpaItemWriterBuilder<TransactionEntity>()
				.entityManagerFactory(entityManagerFactory)
				.build();
	}
}
