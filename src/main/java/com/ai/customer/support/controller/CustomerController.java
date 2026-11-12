package com.ai.customer.support.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai.customer.support.dto.ApiResponse;
import com.ai.customer.support.dto.CustomerRequest;
import com.ai.customer.support.dto.CustomerResponse;
import com.ai.customer.support.dto.CustomerUpdateRequest;
import com.ai.customer.support.service.CustomerService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

	private final CustomerService customerService;

	@PostMapping
	public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
	}

	@GetMapping
	public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
		return ResponseEntity.status(HttpStatus.OK).body(customerService.getAllCustomers());
	}

	@GetMapping("/{id}")
	public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
		return ResponseEntity.status(HttpStatus.OK).body(customerService.getCustomerById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable Long id,
			@Valid @RequestBody CustomerUpdateRequest request) {
		return ResponseEntity.status(HttpStatus.OK).body(customerService.updateCustomer(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse> deleteCustomer(@PathVariable Long id) {

		customerService.deleteCustomer(id);
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Customer deleted successfully"));
	}
}
