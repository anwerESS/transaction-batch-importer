package com.example.transactionbatchimporter.exception;

import com.example.transactionbatchimporter.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BatchJobNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleBatchJobNotFoundException(
			BatchJobNotFoundException ex,
			HttpServletRequest request) {
		return buildResponse(
				HttpStatus.NOT_FOUND,
				"Batch job not found",
				ex.getMessage(),
				request.getRequestURI(),
				List.of("jobExecutionId=" + ex.getJobExecutionId()));
	}

	@ExceptionHandler(InvalidTransactionException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidTransactionException(
			InvalidTransactionException ex,
			HttpServletRequest request) {
		return buildResponse(
				HttpStatus.UNPROCESSABLE_ENTITY,
				"Invalid transaction",
				ex.getMessage(),
				request.getRequestURI(),
				List.of());
	}

	@ExceptionHandler(InvalidInputFileException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidInputFileException(
			InvalidInputFileException ex,
			HttpServletRequest request) {
		return buildResponse(
				HttpStatus.UNPROCESSABLE_ENTITY,
				"Invalid input file",
				ex.getMessage(),
				request.getRequestURI(),
				List.of());
	}

	@ExceptionHandler(BatchImportException.class)
	public ResponseEntity<ApiErrorResponse> handleBatchImportException(
			BatchImportException ex,
			HttpServletRequest request) {
		return buildResponse(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Batch import error",
				ex.getMessage(),
				request.getRequestURI(),
				List.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<String> details = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.toList();

		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"Validation error",
				"Request validation failed.",
				request.getRequestURI(),
				details);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
			ConstraintViolationException ex,
			HttpServletRequest request) {
		List<String> details = ex.getConstraintViolations().stream()
				.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
				.toList();

		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"Validation error",
				"Request validation failed.",
				request.getRequestURI(),
				details);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(
			HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"Malformed request",
				"Request body is missing or malformed.",
				request.getRequestURI(),
				List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
		return buildResponse(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Internal server error",
				"An unexpected error occurred.",
				request.getRequestURI(),
				List.of());
	}

	private ResponseEntity<ApiErrorResponse> buildResponse(
			HttpStatus status,
			String error,
			String message,
			String path,
			List<String> details) {
		ApiErrorResponse response = ApiErrorResponse.builder()
				.timestamp(LocalDateTime.now())
				.status(status.value())
				.error(error)
				.message(message)
				.path(path)
				.details(details)
				.build();

		return ResponseEntity.status(status).body(response);
	}
}
