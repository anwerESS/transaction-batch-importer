package com.example.transactionbatchimporter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record BatchJobResponse(
		Long jobExecutionId,
		String jobName,
		String status,
		LocalDateTime createTime,
		LocalDateTime startTime,
		LocalDateTime endTime,
		String exitCode,
		String exitDescription) {
}
