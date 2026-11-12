package com.ai.customer.support.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ai.customer.support.entity.User;
import com.ai.customer.support.enums.Role;
import com.ai.customer.support.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(String... args) throws Exception {

		if (userRepository.existsByEmail("admin@aisupport.com")) {
			return;
		}

		User admin = User.builder().email("admin@aisupport.com").password(passwordEncoder.encode("Admin@12345"))
				.firstName("System").lastName("Admin").role(Role.ADMIN).active(true).build();

		userRepository.save(admin);

		System.out.println("======================================");
		System.out.println("Initial ADMIN user created");
		System.out.println("Email: admin@aisupport.com");
		System.out.println("======================================");

	}

}
