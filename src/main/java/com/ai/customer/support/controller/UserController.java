package com.ai.customer.support.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai.customer.support.dto.ApiResponse;
import com.ai.customer.support.dto.UserRequest;
import com.ai.customer.support.dto.UserResponse;
import com.ai.customer.support.dto.UserUpdateRequest;
import com.ai.customer.support.service.UserService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

	private final UserService userService;

	@PostMapping
	public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
	}

	@GetMapping
	public ResponseEntity<List<UserResponse>> getAllUsers() {
		return ResponseEntity.status(HttpStatus.OK).body(userService.getAllUsers());
	}

	@GetMapping("/agents")
	public ResponseEntity<List<UserResponse>> getActiveAgents() {
		return ResponseEntity.status(HttpStatus.OK).body(userService.getActiveAgents());
	}

	@GetMapping("/{id}")
	public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
		return ResponseEntity.status(HttpStatus.OK).body(userService.getUserById(id));
	}

	@GetMapping("/email/{email}")
	public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
		return ResponseEntity.status(HttpStatus.OK).body(userService.getUserByEmail(email));
	}

	@PutMapping("/{id}")
	public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
			@Valid @RequestBody UserUpdateRequest request) {
		return ResponseEntity.status(HttpStatus.OK).body(userService.updateUser(id, request));
	}

	@PatchMapping("/{id}/deactivate")
	public ResponseEntity<ApiResponse> deactivateUser(@PathVariable Long id) {
		userService.deactivateUser(id);
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("User deactivated successfully"));
	}

}
