package com.ai.customer.support.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai.customer.support.dto.AuthResponse;
import com.ai.customer.support.dto.LoginRequest;
import com.ai.customer.support.dto.RegisterRequest;
import com.ai.customer.support.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@Operation(summary = "Register a new customer", description = "Creates a new customer account and returns a JWT authentication token.")
	@ApiResponses({ @ApiResponse(responseCode = "201", description = "Customer registered successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid registration data") })
	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		AuthResponse response = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Login", description = "Authenticates a user and returns a JWT token.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Login successful"),
			@ApiResponse(responseCode = "400", description = "Invalid login request"),
			@ApiResponse(responseCode = "401", description = "Invalid email or password") })
	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		AuthResponse response = authService.login(request);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

}
