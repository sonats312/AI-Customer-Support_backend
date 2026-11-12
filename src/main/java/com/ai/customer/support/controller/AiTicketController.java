package com.ai.customer.support.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai.customer.support.dto.TicketResponse;
import com.ai.customer.support.service.ai.AiTicketService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai/tickets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AiTicketController {

	private final AiTicketService aiTicketService;

	@PostMapping("/{ticketId}/analyze")
	public ResponseEntity<TicketResponse> analyzeTicket(@PathVariable Long ticketId, Authentication authentication) {
		return ResponseEntity.status(HttpStatus.OK).body(aiTicketService.analyzeTicket(ticketId, authentication));
	}
}
