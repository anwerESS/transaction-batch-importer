package com.example.transactionbatchimporter.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.transactionbatchimporter.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TransactionImportJobIntegrationTest {

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private JobExplorer jobExplorer;

	@Autowired
	private Job importTransactionsJob;

	@Autowired
	private TransactionRepository transactionRepository;

	@BeforeEach
	void setUp() {
		transactionRepository.deleteAll();
	}

	@Test
	void importingSameCsvTwiceShouldNotCreateDuplicateTransactions() throws Exception {
		JobExecution firstExecution = launchJob();
		JobExecution secondExecution = launchJob();

		assertThat(firstExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(secondExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(transactionRepository.count()).isEqualTo(30);
	}

	private JobExecution launchJob() throws Exception {
		JobExecution jobExecution = jobLauncher.run(
				importTransactionsJob,
				new JobParametersBuilder()
						.addString("fileName", "transactions.csv")
						.addLong("startedAt", System.nanoTime())
						.toJobParameters());
		return waitForJobCompletion(jobExecution.getId());
	}

	private JobExecution waitForJobCompletion(Long jobExecutionId) throws InterruptedException {
		long timeoutAt = System.currentTimeMillis() + 60_000;
		JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);

		while (jobExecution != null && jobExecution.getStatus().isRunning()
				&& System.currentTimeMillis() < timeoutAt) {
			Thread.sleep(100);
			jobExecution = jobExplorer.getJobExecution(jobExecutionId);
		}

		assertThat(jobExecution).isNotNull();
		assertThat(jobExecution.getStatus()).isNotEqualTo(BatchStatus.STARTING);
		assertThat(jobExecution.getStatus()).isNotEqualTo(BatchStatus.STARTED);
		return jobExecution;
	}
}
