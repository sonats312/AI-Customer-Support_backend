package com.ai.customer.support.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ai.customer.support.dto.AuthResponse;
import com.ai.customer.support.dto.LoginRequest;
import com.ai.customer.support.dto.RegisterRequest;
import com.ai.customer.support.entity.User;
import com.ai.customer.support.enums.Role;
import com.ai.customer.support.exception.BadRequestException;
import com.ai.customer.support.repository.UserRepository;
import com.ai.customer.support.security.CustomUserDetails;
import com.ai.customer.support.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthResponse register(RegisterRequest request) {

		log.info("Registration attempt for email: {}", request.email());

		if (userRepository.existsByEmail(request.email())) {
			log.warn("Registration failed - user already exists with email: {}", request.email());
			throw new BadRequestException("User already exists with email: " + request.email());
		}

		// Public registration can ONLY create customers.For Role
		User user = User.builder().email(request.email()).password(passwordEncoder.encode(request.password()))
				.firstName(request.firstName()).lastName(request.lastName()).role(Role.CUSTOMER).active(true).build();

		User savedUser = userRepository.save(user);
		log.info("Customer registered successfully - userId: {}, email: {}", savedUser.getId(), savedUser.getEmail());

		CustomUserDetails userDetails = new CustomUserDetails(savedUser);

		String token = jwtService.generateToken(userDetails);
		log.debug("JWT generated successfully for newly registered userId: {}", savedUser.getId());

		return buildAuthResponse(savedUser, token);
	}

	public AuthResponse login(LoginRequest request) {

		log.info("Login attempt for email: {}", request.email());

		Authentication authentication = null;

		try {

			authentication = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));

		} catch (Exception e) {
			log.warn("Login failed for email: {} - authentication failed", request.email());
			throw new AccessDeniedException("Authentication failed", e);
		}

		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

		User user = userDetails.getUser();

		if (!user.getActive()) {
			log.warn("Login rejected - inactive user account. userId: {}, email: {}", user.getId(), user.getEmail());
			throw new BadRequestException("User account is inactive");
		}

		String token = jwtService.generateToken(userDetails);

		log.info("Login successful - userId: {}, email: {}, role: {}", user.getId(), user.getEmail(), user.getRole());
		log.debug("JWT generated successfully for userId: {}", user.getId());

		return buildAuthResponse(user, token);
	}

	private AuthResponse buildAuthResponse(User user, String token) {
		return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
				user.getRole().name());
	}

}
