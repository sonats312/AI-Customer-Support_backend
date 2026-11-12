package com.ai.customer.support.repository;

import java.util.Optional;

//import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ai.customer.support.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
	boolean existsByEmail(String email);
	Optional<Customer> findByEmail(String email);
}
