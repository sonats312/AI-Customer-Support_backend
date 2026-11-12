package com.ai.customer.support.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ai.customer.support.security.CustomUserDetailsService;
import com.ai.customer.support.security.JwtAuthenticationFilter;
import com.ai.customer.support.security.RestAccessDeniedHandler;
import com.ai.customer.support.security.RestAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final RestAuthenticationEntryPoint authenticationEntryPoint;
	private final RestAccessDeniedHandler accessDeniedHandler;
	private final CustomUserDetailsService userDetailsService;
	private final PasswordEncoder passwordEncoder;

	@Bean
	AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {

		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowedOrigins(List.of("http://localhost:4200"));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.csrf(csrf -> csrf.disable()).cors(cors -> {
		}).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(authenticationEntryPoint).accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(auth -> auth

						// Public authentication APIs
						.requestMatchers("/api/auth/**").permitAll()

						// Swagger / OpenAPI
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

						.requestMatchers(HttpMethod.GET, "/api/users/agents").hasAnyRole("ADMIN", "AGENT")
						// Admin only
						.requestMatchers("/api/users/**").hasRole("ADMIN")

						// Customer APIs
						.requestMatchers(HttpMethod.GET, "/api/customers/**").hasAnyRole("ADMIN", "AGENT", "CUSTOMER")
						.requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "AGENT")

						// Ticket APIs
						.requestMatchers(HttpMethod.POST, "/api/tickets").hasAnyRole("ADMIN", "AGENT", "CUSTOMER")
						.requestMatchers(HttpMethod.GET, "/api/tickets/**").hasAnyRole("ADMIN", "AGENT", "CUSTOMER")
						.requestMatchers(HttpMethod.PATCH, "/api/tickets/**").hasAnyRole("ADMIN", "AGENT")
						.requestMatchers(HttpMethod.DELETE, "/api/tickets/**").hasAnyRole("ADMIN", "AGENT")

						// Conversation APIs
						.requestMatchers("/api/conversations/**").hasAnyRole("ADMIN", "AGENT", "CUSTOMER")

						// AI Agent Assist
						.requestMatchers("/api/ai/**").hasAnyRole("ADMIN", "AGENT")

						// Everything else requires authentication
						.anyRequest().authenticated())

				.authenticationProvider(authenticationProvider())

				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

}