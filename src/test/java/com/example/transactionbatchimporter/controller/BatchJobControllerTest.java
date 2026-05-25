package com.example.transactionbatchimporter.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.transactionbatchimporter.exception.GlobalExceptionHandler;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BatchJobController.class)
@Import(GlobalExceptionHandler.class)
class BatchJobControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JobLauncher jobLauncher;

	@MockitoBean
	private JobExplorer jobExplorer;

	@MockitoBean
	private Job importTransactionsJob;

	@Test
	void importTransactionsShouldReturnAcceptedWithStatusLocation() throws Exception {
		JobExecution jobExecution = jobExecution(2L, BatchStatus.STARTING);
		given(jobLauncher.run(eq(importTransactionsJob), any(JobParameters.class))).willReturn(jobExecution);

		mockMvc.perform(post("/api/batch/transactions/import"))
				.andExpect(status().isAccepted())
				.andExpect(header().string("Location", "http://localhost/api/batch/jobs/2"))
				.andExpect(jsonPath("$.jobExecutionId").value(2))
				.andExpect(jsonPath("$.jobName").value("importTransactionsJob"))
				.andExpect(jsonPath("$.status").value("STARTING"));
	}

	@Test
	void getJobExecutionShouldReturnCurrentStatus() throws Exception {
		JobExecution jobExecution = jobExecution(2L, BatchStatus.COMPLETED);
		jobExecution.setEndTime(LocalDateTime.of(2026, 5, 25, 17, 30));
		jobExecution.setExitStatus(ExitStatus.COMPLETED);
		given(jobExplorer.getJobExecution(2L)).willReturn(jobExecution);

		mockMvc.perform(get("/api/batch/jobs/2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.jobExecutionId").value(2))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.exitCode").value("COMPLETED"));
	}

	@Test
	void getJobExecutionShouldReturnNotFoundForUnknownExecution() throws Exception {
		given(jobExplorer.getJobExecution(404L)).willReturn(null);

		mockMvc.perform(get("/api/batch/jobs/404"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Batch job not found"))
				.andExpect(jsonPath("$.details[0]").value("jobExecutionId=404"));
	}

	private JobExecution jobExecution(long id, BatchStatus status) {
		JobInstance jobInstance = new JobInstance(1L, "importTransactionsJob");
		JobExecution jobExecution = new JobExecution(jobInstance, id, new JobParameters());
		jobExecution.setStatus(status);
		jobExecution.setCreateTime(LocalDateTime.of(2026, 5, 25, 17, 24));
		jobExecution.setStartTime(LocalDateTime.of(2026, 5, 25, 17, 24, 1));
		return jobExecution;
	}
}
