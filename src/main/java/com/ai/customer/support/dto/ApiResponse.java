package com.ai.customer.support.dto;

import java.time.LocalDateTime;

public record ApiResponse(boolean success, String message, LocalDateTime timestamp) {

	public static ApiResponse success(String message) {
		return new ApiResponse(true, message, LocalDateTime.now());
	}

	public static ApiResponse failure(String message) {
		return new ApiResponse(false, message, LocalDateTime.now());
	}
}
