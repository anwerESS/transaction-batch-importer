package com.example.transactionbatchimporter.batch.listener;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChunkProgressListener implements ChunkListener {

	private static final String START_READ_COUNT = "chunk.startReadCount";
	private static final String START_WRITE_COUNT = "chunk.startWriteCount";
	private static final Duration DEMO_CHUNK_DELAY = Duration.ofSeconds(2);

	@Override
	public void beforeChunk(ChunkContext context) {
		StepExecution stepExecution = context.getStepContext().getStepExecution();
		context.setAttribute(START_READ_COUNT, stepExecution.getReadCount());
		context.setAttribute(START_WRITE_COUNT, stepExecution.getWriteCount());

		// Commenter cette ligne pour desactiver le delai de demo.
		sleepForDemoDelay(stepExecution);
	}

	@Override
	public void afterChunk(ChunkContext context) {
		StepExecution stepExecution = context.getStepContext().getStepExecution();
		long chunkReadCount = stepExecution.getReadCount() - (long) context.getAttribute(START_READ_COUNT);
		long chunkWriteCount = stepExecution.getWriteCount() - (long) context.getAttribute(START_WRITE_COUNT);
		if (chunkReadCount == 0 && chunkWriteCount == 0) {
			return;
		}

		log.info("Completed chunk {} for step {}. chunkReadCount={}, chunkWriteCount={}, totalReadCount={}, totalWriteCount={}.",
				stepExecution.getCommitCount(),
				stepExecution.getStepName(),
				chunkReadCount,
				chunkWriteCount,
				stepExecution.getReadCount(),
				stepExecution.getWriteCount());
	}

	private void sleepForDemoDelay(StepExecution stepExecution) {
		try {
			log.info("Demo delay before next chunk for step {}: {} seconds.",
					stepExecution.getStepName(),
					DEMO_CHUNK_DELAY.toSeconds());
			Thread.sleep(DEMO_CHUNK_DELAY.toMillis());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Demo delay was interrupted.", ex);
		}
	}
}
