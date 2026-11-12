package com.ai.customer.support.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException exception) {
		return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException exception) {
		return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {

		Map<String, Object> response = new HashMap<>();

		response.put("timestamp", LocalDateTime.now());
		response.put("status", HttpStatus.BAD_REQUEST.value());
		response.put("error", "Validation failed");

		Map<String, String> errors = new HashMap<>();

		exception.getBindingResult().getFieldErrors()
				.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
		response.put("errors", errors);

		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(AiAnalysisException.class)
	public ResponseEntity<Map<String, Object>> handleAiAnalysisException(AiAnalysisException exception,
			HttpServletRequest request) {

		Map<String, Object> body = new LinkedHashMap<>();

		body.put("timestamp", LocalDateTime.now());
		body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
		body.put("error", "AI Service Unavailable");
		body.put("message", exception.getMessage());
		body.put("path", request.getRequestURI());

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGeneralException(Exception exception, HttpServletRequest request) {
		exception.printStackTrace();

		Map<String, Object> body = new LinkedHashMap<>();

		body.put("timestamp", LocalDateTime.now());
		body.put("status", 500);
		body.put("error", "Internal Server Error");
		body.put("message", exception.getMessage());
		body.put("exception", exception.getClass().getName());
		body.put("path", request.getRequestURI());

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
//		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex,
			HttpServletRequest request) {

		Map<String, Object> body = new LinkedHashMap<>();

		body.put("timestamp", LocalDateTime.now());
		body.put("status", 403);
		body.put("error", "Forbidden");
		body.put("message", ex.getMessage());
		body.put("path", request.getRequestURI());

		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
	}

	private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {

		Map<String, Object> response = new HashMap<>();

		response.put("timestamp", LocalDateTime.now());
		response.put("status", status.value());
		response.put("error", message);

		return ResponseEntity.status(status).body(response);
	}

}
