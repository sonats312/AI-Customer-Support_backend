package com.ai.customer.support.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ai.customer.support.dto.UserRequest;
import com.ai.customer.support.dto.UserResponse;
import com.ai.customer.support.dto.UserUpdateRequest;
import com.ai.customer.support.entity.User;
import com.ai.customer.support.enums.Role;
import com.ai.customer.support.exception.BadRequestException;
import com.ai.customer.support.exception.ResourceNotFoundException;
import com.ai.customer.support.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserResponse createUser(UserRequest request) {

		if (userRepository.existsByEmail(request.email())) {
			throw new BadRequestException("User with this email already exists");
		}

		User user = User.builder().email(request.email()).password(passwordEncoder.encode(request.password()))
				.firstName(request.firstName()).lastName(request.lastName())
				.role(request.role() != null ? request.role() : Role.AGENT).active(true).build();
		log.info("Created User: {}", user);

		return toResponse(userRepository.save(user));
	}

	public List<UserResponse> getAllUsers() {
		return userRepository.findAll().stream().map(this::toResponse).toList();
	}

	public List<UserResponse> getActiveAgents() {
		return userRepository.findByRoleAndActiveTrue(Role.AGENT).stream().map(this::toResponse).toList();
	}

	public UserResponse getUserById(Long id) {
		return toResponse(findUser(id));
	}

	public UserResponse getUserByEmail(String email) {

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

		return toResponse(user);
	}

	public UserResponse updateUser(Long id, UserUpdateRequest request) {

		User user = findUser(id);

		user.setFirstName(request.firstName());
		user.setLastName(request.lastName());
		user.setRole(request.role());
		user.setActive(request.active());

		return toResponse(userRepository.save(user));
	}

	public void deactivateUser(Long id) {
		User user = findUser(id);
		user.setActive(false);
		userRepository.save(user);
	}

	private User findUser(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
	}

	private UserResponse toResponse(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole(),
				user.getActive());
	}
}
