package com.example.transactionbatchimporter.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.transactionbatchimporter.repository.TransactionRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.FileSystemUtils;

@SpringBootTest
class TransactionImportJobIntegrationTest {

	private static final Path TEST_WORK_DIRECTORY = createTestWorkDirectory();
	private static final Path TEST_INPUT_DIRECTORY = TEST_WORK_DIRECTORY.resolve("input");
	private static final Path TEST_ARCHIVE_DIRECTORY = TEST_WORK_DIRECTORY.resolve("archive");
	private static final Path TEST_REPORT_DIRECTORY = TEST_WORK_DIRECTORY.resolve("reports");
	private static final Path SAMPLE_INPUT_FILE = Path.of("src/main/resources/input/transactions.csv.backup");

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private JobExplorer jobExplorer;

	@Autowired
	private Job importTransactionsJob;

	@Autowired
	private TransactionRepository transactionRepository;

	@DynamicPropertySource
	static void registerBatchFileProperties(DynamicPropertyRegistry registry) {
		registry.add("app.transaction-import.input-directory", () -> TEST_INPUT_DIRECTORY.toString());
		registry.add("app.transaction-import.archive-directory", () -> TEST_ARCHIVE_DIRECTORY.toString());
		registry.add("app.transaction-import.report-directory", () -> TEST_REPORT_DIRECTORY.toString());
	}

	@AfterAll
	static void cleanUp() throws IOException {
		FileSystemUtils.deleteRecursively(TEST_WORK_DIRECTORY);
	}

	@BeforeEach
	void setUp() throws IOException {
		transactionRepository.deleteAll();
		FileSystemUtils.deleteRecursively(TEST_WORK_DIRECTORY);
		Files.createDirectories(TEST_INPUT_DIRECTORY);
		Files.createDirectories(TEST_ARCHIVE_DIRECTORY);
		Files.createDirectories(TEST_REPORT_DIRECTORY);
	}

	@Test
	void importingSameCsvTwiceShouldNotCreateDuplicateTransactions() throws Exception {
		prepareInputFile();
		JobExecution firstExecution = launchJob();

		prepareInputFile();
		JobExecution secondExecution = launchJob();

		assertThat(firstExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(secondExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(transactionRepository.count()).isEqualTo(30);
	}

	@Test
	void importJobShouldValidateImportReportAndArchiveInputFile() throws Exception {
		prepareInputFile();

		JobExecution jobExecution = launchJob();

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(transactionRepository.count()).isEqualTo(30);
		assertThat(TEST_INPUT_DIRECTORY.resolve("transactions.csv")).doesNotExist();
		assertThat(reportContent(jobExecution))
				.contains(
						"jobExecutionId=" + jobExecution.getId(),
						"jobName=importTransactionsJob",
						"status=COMPLETED",
						"readCount=30",
						"writeCount=30",
						"skipCount=0",
						"rollbackCount=0");
		assertThat(archivedFileNames())
				.anySatisfy(fileName -> assertThat(fileName)
						.startsWith("transactions-job-" + jobExecution.getId() + "-")
						.endsWith(".csv"));
	}

	@Test
	void importJobShouldFailBeforeImportWhenHeaderIsInvalid() throws Exception {
		Files.writeString(
				TEST_INPUT_DIRECTORY.resolve("transactions.csv"),
				"wrong,header\n",
				StandardCharsets.UTF_8);

		JobExecution jobExecution = launchJob();

		assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
		assertThat(transactionRepository.count()).isZero();
		assertThat(TEST_INPUT_DIRECTORY.resolve("transactions.csv")).exists();
		assertThat(archivedFileNames()).isEmpty();
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

	private void prepareInputFile() throws IOException {
		Files.copy(
				SAMPLE_INPUT_FILE,
				TEST_INPUT_DIRECTORY.resolve("transactions.csv"),
				StandardCopyOption.REPLACE_EXISTING);
	}

	private String reportContent(JobExecution jobExecution) throws IOException {
		Path reportFile = TEST_REPORT_DIRECTORY.resolve("transaction-import-report-" + jobExecution.getId() + ".txt");
		assertThat(reportFile).exists();
		return Files.readString(reportFile);
	}

	private List<String> archivedFileNames() throws IOException {
		try (Stream<Path> files = Files.list(TEST_ARCHIVE_DIRECTORY)) {
			return files
					.map(path -> path.getFileName().toString())
					.toList();
		}
	}

	private static Path createTestWorkDirectory() {
		try {
			return Files.createTempDirectory("transaction-batch-importer-test-");
		} catch (IOException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}
}
