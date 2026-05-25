package com.example.transactionbatchimporter.config;

import com.example.transactionbatchimporter.batch.tasklet.ArchiveInputFileTasklet;
import com.example.transactionbatchimporter.batch.tasklet.GenerateImportReportTasklet;
import com.example.transactionbatchimporter.batch.tasklet.ValidateInputFileTasklet;
import com.example.transactionbatchimporter.dto.TransactionCsvDto;
import com.example.transactionbatchimporter.entity.TransactionEntity;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TransactionImportJobConfig {

	// Declare le job Spring Batch responsable de l'import des transactions.
	@Bean
	public Job importTransactionsJob(
			JobRepository jobRepository,
			Step validateInputFileStep,
			Step importTransactionsStep,
			Step generateImportReportStep,
			Step archiveInputFileStep,
			JobExecutionListener jobCompletionNotificationListener) {
		// Le job execute les steps dans l'ordre et s'arrete automatiquement si une step echoue.
		return new JobBuilder("importTransactionsJob", jobRepository)
				.listener(jobCompletionNotificationListener)
				.start(validateInputFileStep)
				.next(importTransactionsStep)
				.next(generateImportReportStep)
				.next(archiveInputFileStep)
				.build();
	}

	// Verifie le fichier CSV avant de demarrer la lecture chunk-oriented.
	@Bean
	public Step validateInputFileStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			ValidateInputFileTasklet validateInputFileTasklet) {
		return new StepBuilder("validateInputFileStep", jobRepository)
				.tasklet(validateInputFileTasklet, transactionManager)
				.build();
	}

	// Declare l'etape qui lit le CSV, transforme les lignes et persiste les transactions.
	@Bean
	public Step importTransactionsStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<TransactionCsvDto> transactionCsvItemReader,
			ItemProcessor<TransactionCsvDto, TransactionEntity> transactionItemProcessor,
			JpaItemWriter<TransactionEntity> transactionJpaItemWriter,
			ChunkListener chunkProgressListener) {
		// Spring Batch lit, traite et ecrit les donnees par paquets de 10 lignes CSV.
		return new StepBuilder("importTransactionsStep", jobRepository)
				// Definit le type lu depuis le CSV et le type ecrit en base.
				.<TransactionCsvDto, TransactionEntity>chunk(10, transactionManager)
				// Lit les transactions depuis le fichier CSV.
				.reader(transactionCsvItemReader)
				// Transforme chaque DTO CSV en entite JPA.
				.processor(transactionItemProcessor)
				// Enregistre les entites transformees en base de donnees.
				.writer(transactionJpaItemWriter)
				// Suit l'avancement de chaque chunk pendant l'import.
				.listener(chunkProgressListener)
				.build();
	}

	// Genere un rapport texte avec les metadonnees de l'execution et les compteurs d'import.
	@Bean
	public Step generateImportReportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			GenerateImportReportTasklet generateImportReportTasklet) {
		return new StepBuilder("generateImportReportStep", jobRepository)
				.tasklet(generateImportReportTasklet, transactionManager)
				.build();
	}

	// Deplace le fichier traite vers le dossier d'archive apres un import reussi.
	@Bean
	public Step archiveInputFileStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			ArchiveInputFileTasklet archiveInputFileTasklet) {
		return new StepBuilder("archiveInputFileStep", jobRepository)
				.tasklet(archiveInputFileTasklet, transactionManager)
				.build();
	}
}
