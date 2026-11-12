package com.ai.customer.support.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ai.customer.support.dto.CustomerRequest;
import com.ai.customer.support.dto.CustomerResponse;
import com.ai.customer.support.dto.CustomerUpdateRequest;
import com.ai.customer.support.entity.Customer;
import com.ai.customer.support.exception.BadRequestException;
import com.ai.customer.support.exception.ResourceNotFoundException;
import com.ai.customer.support.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

	private final CustomerRepository customerRepository;

	public CustomerResponse createCustomer(CustomerRequest request) {

		if (customerRepository.existsByEmail(request.email())) {
			throw new BadRequestException("Customer with this email already exists");
		}

		Customer customer = Customer.builder().name(request.name()).email(request.email()).phone(request.phone())
				.company(request.company()).build();

		return toResponse(customerRepository.save(customer));
	}

	public List<CustomerResponse> getAllCustomers() {
		return customerRepository.findAll().stream().map(this::toResponse).toList();
	}

	public CustomerResponse getCustomerById(Long id) {

		Customer customer = customerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
		return toResponse(customer);
	}

	public CustomerResponse updateCustomer(Long id, CustomerUpdateRequest request) {

		Customer customer = customerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

		customer.setName(request.name());
		customer.setEmail(request.email());
		customer.setPhone(request.phone());
		customer.setCompany(request.company());

		return toResponse(customerRepository.save(customer));
	}

	public void deleteCustomer(Long id) {

		Customer customer = customerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

		customerRepository.delete(customer);
	}

	private CustomerResponse toResponse(Customer customer) {
		return new CustomerResponse(customer.getId(), customer.getName(), customer.getEmail(), customer.getPhone(),
				customer.getCompany(), customer.getCreatedAt());
	}
}
