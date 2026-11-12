package com.ai.customer.support.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;

import com.ai.customer.support.dto.LoginRequest;
import com.ai.customer.support.dto.AuthResponse;
import com.ai.customer.support.dto.RegisterRequest;
import com.ai.customer.support.entity.User;
import com.ai.customer.support.enums.Role;
import com.ai.customer.support.exception.BadRequestException;
import com.ai.customer.support.repository.UserRepository;
import com.ai.customer.support.security.CustomUserDetails;
import com.ai.customer.support.security.JwtService;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtService jwtService;

	@InjectMocks
	private AuthService authService;

	@Test
	void register_shouldRegisterCustomerSuccessfully() {

		RegisterRequest request = new RegisterRequest("customer@example.com", "Password123!", "John", "Customer");
		User savedUser = User.builder().id(1L).email("customer@example.com").password("encoded-password")
				.firstName("John").lastName("Customer").role(Role.CUSTOMER).active(true).build();

		when(userRepository.existsByEmail("customer@example.com")).thenReturn(false);

		when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");

		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("test-jwt-token");

		AuthResponse response = authService.register(request);

		assertNotNull(response);
		assertEquals("test-jwt-token", response.accessToken());
		assertEquals("Bearer", response.tokenType());
		assertEquals(1L, response.userId());
		assertEquals("customer@example.com", response.email());
		assertEquals("John", response.firstName());
		assertEquals("Customer", response.lastName());
		assertEquals("CUSTOMER", response.role());

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

		verify(userRepository).save(userCaptor.capture());

		User userToSave = userCaptor.getValue();

		assertEquals("customer@example.com", userToSave.getEmail());
		assertEquals("encoded-password", userToSave.getPassword());
		assertEquals(Role.CUSTOMER, userToSave.getRole());
		assertEquals(true, userToSave.getActive());

		verify(passwordEncoder).encode("Password123!");
		verify(jwtService).generateToken(any(CustomUserDetails.class));
	}

	@Test
	void register_shouldThrowExceptionWhenEmailAlreadyExists() {

		RegisterRequest request = new RegisterRequest("customer@example.com", "Password123!", "John", "Customer");

		when(userRepository.existsByEmail("customer@example.com")).thenReturn(true);

		BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.register(request));

		assertEquals("User already exists with email: customer@example.com", exception.getMessage());

		verify(userRepository).existsByEmail("customer@example.com");
		verify(userRepository, never()).save(any(User.class));
		verify(passwordEncoder, never()).encode(any(String.class));
		verify(jwtService, never()).generateToken(any(CustomUserDetails.class));
	}

	@Test
	void register_shouldEncodePasswordBeforeSavingUser() {
		RegisterRequest request = new RegisterRequest("customer@example.com", "Password123!", "John", "Customer");

		when(userRepository.existsByEmail("customer@example.com")).thenReturn(false);
		when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			user.setId(1L);
			return user;
		});

		when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("test-jwt-token");

		authService.register(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

		verify(passwordEncoder).encode("Password123!");
		verify(userRepository).save(userCaptor.capture());

		User savedUser = userCaptor.getValue();

		assertEquals("encoded-password", savedUser.getPassword());
		assertNotNull(savedUser.getPassword());
		assertEquals(false, "Password123!".equals(savedUser.getPassword()));
	}

	@Test
	void register_shouldAlwaysAssignCustomerRole() {
		RegisterRequest request = new RegisterRequest("customer@example.com", "Password123!", "John", "Customer");

		when(userRepository.existsByEmail("customer@example.com")).thenReturn(false);
		when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			user.setId(1L);
			return user;
		});

		when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("test-jwt-token");

		authService.register(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

		verify(userRepository).save(userCaptor.capture());

		assertEquals(Role.CUSTOMER, userCaptor.getValue().getRole());
		assertEquals(true, userCaptor.getValue().getActive());
	}

	@Test
	void register_shouldGenerateJwtTokenAfterSuccessfulRegistration() {
		RegisterRequest request = new RegisterRequest("customer@example.com", "Password123!", "John", "Customer");

		when(userRepository.existsByEmail(request.email())).thenReturn(false);

		when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User savedUser = invocation.getArgument(0);
			savedUser.setId(1L);
			return savedUser;
		});

		when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("test-jwt-token");

		AuthResponse response = authService.register(request);

		assertNotNull(response);
		assertEquals("test-jwt-token", response.accessToken());
		assertEquals("Bearer", response.tokenType());
		assertEquals(1L, response.userId());

		verify(jwtService).generateToken(any(CustomUserDetails.class));
	}

	@Test
	void register_shouldReturnCorrectCustomerDetails() {
		RegisterRequest request = new RegisterRequest("customer@example.com", "Password123!", "John", "Customer");

		when(userRepository.existsByEmail(request.email())).thenReturn(false);

		when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User savedUser = invocation.getArgument(0);
			savedUser.setId(1L);
			return savedUser;
		});

		when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("test-jwt-token");

		AuthResponse response = authService.register(request);

		assertEquals(1L, response.userId());
		assertEquals("customer@example.com", response.email());
		assertEquals("John", response.firstName());
		assertEquals("Customer", response.lastName());
		assertEquals("CUSTOMER", response.role());
		assertEquals("Bearer", response.tokenType());
		assertEquals("test-jwt-token", response.accessToken());
	}

	@Test
	void login_shouldAuthenticateUserSuccessfully() {
		LoginRequest request = new LoginRequest("customer@example.com", "Password123!");

		User user = User.builder().id(1L).email("customer@example.com").password("encoded-password").firstName("John")
				.lastName("Customer").role(Role.CUSTOMER).active(true).build();

		CustomUserDetails userDetails = new CustomUserDetails(user);

		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
				userDetails.getAuthorities());

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(authentication);

		when(jwtService.generateToken(userDetails)).thenReturn("test-jwt-token");

		AuthResponse response = authService.login(request);

		assertNotNull(response);
		assertEquals("test-jwt-token", response.accessToken());
		assertEquals("Bearer", response.tokenType());
		assertEquals(1L, response.userId());
		assertEquals("customer@example.com", response.email());
		assertEquals("CUSTOMER", response.role());

		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verify(jwtService).generateToken(userDetails);
	}

	@Test
	void login_shouldThrowAccessDeniedExceptionWhenAuthenticationFails() {
		LoginRequest request = new LoginRequest("customer@example.com", "WrongPassword123!");

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenThrow(new BadCredentialsException("Bad credentials"));

		assertThrows(AccessDeniedException.class, () -> authService.login(request));

		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

		verify(jwtService, never()).generateToken(any(CustomUserDetails.class));
	}

	@Test
	void login_shouldRejectInactiveUser() {
		LoginRequest request = new LoginRequest("customer@example.com", "Password123!");

		User user = User.builder().id(1L).email("customer@example.com").password("encodedPassword").firstName("John")
				.lastName("Customer").role(Role.CUSTOMER).active(false).build();

		CustomUserDetails userDetails = new CustomUserDetails(user);

		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
				userDetails.getAuthorities());

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(authentication);

		BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.login(request));

		assertEquals("User account is inactive", exception.getMessage());

		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verify(jwtService, never()).generateToken(any(CustomUserDetails.class));
	}

	@Test
	void login_shouldGenerateJwtTokenAfterSuccessfulAuthentication() {
		LoginRequest request = new LoginRequest("customer@example.com", "Password123!");

		User user = User.builder().id(1L).email("customer@example.com").password("encodedPassword").firstName("John")
				.lastName("Customer").role(Role.CUSTOMER).active(true).build();

		CustomUserDetails userDetails = new CustomUserDetails(user);

		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
				userDetails.getAuthorities());

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(authentication);

		when(jwtService.generateToken(userDetails)).thenReturn("test-jwt-token");

		AuthResponse response = authService.login(request);

		assertNotNull(response);
		assertEquals("test-jwt-token", response.accessToken());

		verify(jwtService).generateToken(userDetails);
	}

	@Test
	void login_shouldReturnCorrectUserDetails() {
		LoginRequest request = new LoginRequest("customer@example.com", "Password123!");

		User user = User.builder().id(1L).email("customer@example.com").password("encodedPassword").firstName("John")
				.lastName("Customer").role(Role.CUSTOMER).active(true).build();

		CustomUserDetails userDetails = new CustomUserDetails(user);

		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
				userDetails.getAuthorities());

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(authentication);

		when(jwtService.generateToken(userDetails)).thenReturn("test-jwt-token");

		AuthResponse response = authService.login(request);

		assertEquals(1L, response.userId());
		assertEquals("customer@example.com", response.email());
		assertEquals("John", response.firstName());
		assertEquals("Customer", response.lastName());
		assertEquals("CUSTOMER", response.role());
		assertEquals("Bearer", response.tokenType());

		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
	}

}
