package com.ai.customer.support.dto;

public record AuthResponse(String accessToken, String tokenType, Long userId, String email, String firstName,
		String lastName, String role) {
}
