package com.ai.customer.support.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		Map<String, Object> body = Map.of("timestamp", LocalDateTime.now().toString(), "status", 401, "error",
				"Unauthorized", "message", "Authentication is required", "path", request.getRequestURI());

		objectMapper.writeValue(response.getOutputStream(), body);

	}

}
