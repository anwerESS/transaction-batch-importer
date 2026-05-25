package com.example.transactionbatchimporter.config;

import org.springframework.boot.autoconfigure.batch.BatchTaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class BatchJobLauncherConfig {

	// Déclare l'exécuteur utilisé par Spring Batch pour lancer les jobs.
	@Bean
	@BatchTaskExecutor
	public ThreadPoolTaskExecutor batchTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();

		// Nombre de threads conservés en permanence pour traiter les jobs batch.
		taskExecutor.setCorePoolSize(2);

		// Nombre maximal de threads pouvant être créés en cas de charge.
		taskExecutor.setMaxPoolSize(4);

		// Nombre de jobs pouvant attendre lorsqu'aucun thread n'est disponible.
		taskExecutor.setQueueCapacity(10);

		// Préfixe appliqué aux noms des threads pour les identifier dans les logs.
		taskExecutor.setThreadNamePrefix("batch-job-");

		return taskExecutor;
	}
}
