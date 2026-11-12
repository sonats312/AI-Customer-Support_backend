package com.ai.customer.support.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ai.customer.support.dto.AuthResponse;
import com.ai.customer.support.dto.LoginRequest;
import com.ai.customer.support.service.AuthService;
import com.ai.customer.support.dto.RegisterRequest;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

	@Mock
	private AuthService authService;

	@InjectMocks
	private AuthController authController;

	@Test
	void login_shouldReturnOkResponse() {
		LoginRequest request = new LoginRequest("customer@example.com", "Password123!");

		AuthResponse expectedResponse = new AuthResponse("test-jwt-token", "Bearer", 1L, "customer@example.com", "John",
				"Customer", "CUSTOMER");

		when(authService.login(request)).thenReturn(expectedResponse);

		ResponseEntity<AuthResponse> response = authController.login(request);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("test-jwt-token", response.getBody().accessToken());
		assertEquals("customer@example.com", response.getBody().email());

		verify(authService).login(request);
	}

	@Test
	void register_shouldReturnCreatedResponse() {
		RegisterRequest request = new RegisterRequest("John", "Customer", "customer@example.com", "Password123!");

		AuthResponse expectedResponse = new AuthResponse("test-jwt-token", "Bearer", 1L, "customer@example.com", "John",
				"Customer", "CUSTOMER");

		when(authService.register(request)).thenReturn(expectedResponse);

		ResponseEntity<AuthResponse> response = authController.register(request);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("test-jwt-token", response.getBody().accessToken());
		assertEquals("customer@example.com", response.getBody().email());

		verify(authService).register(request);
	}
}
