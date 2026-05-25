package com.example.transactionbatchimporter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
		LocalDateTime timestamp,
		int status,
		String error,
		String message,
		String path,
		List<String> details) {
}
