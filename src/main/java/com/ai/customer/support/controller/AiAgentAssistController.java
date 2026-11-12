package com.ai.customer.support.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai.customer.support.dto.AiAnalysisResponse;
import com.ai.customer.support.dto.AiAssistRequest;
import com.ai.customer.support.dto.AiAssistResponse;
import com.ai.customer.support.dto.ConversationResponse;
import com.ai.customer.support.service.ai.AiAgentAssistService;
import com.ai.customer.support.service.ai.AiTicketAnalysisService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AiAgentAssistController {

	private final AiAgentAssistService aiAgentAssistService;
	private final AiTicketAnalysisService aiTicketAnalysisService;

	@Operation(summary = "Analyze a customer message", description = "Provides AI-assisted sentiment, intent, category, priority and suggested reply.")
	@PostMapping("/assist")
	@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
	public ResponseEntity<AiAssistResponse> analyze(@Valid @RequestBody AiAssistRequest request) {
		return ResponseEntity.status(HttpStatus.OK).body(aiAgentAssistService.analyze(request.message()));
	}

	@Operation(summary = "Analyze a complete support ticket", description = "Analyzes the ticket description and complete conversation history and saves the AI analysis.")
	@PostMapping("/ticket/{ticketId}/analyze")
	@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
	public ResponseEntity<AiAnalysisResponse> analyzeTicket(@PathVariable Long ticketId,
			Authentication authentication) {
		return ResponseEntity.status(HttpStatus.OK)
				.body(aiTicketAnalysisService.analyzeTicket(ticketId, authentication));
	}

	@Operation(summary = "Get saved AI analysis", description = "Returns the latest saved AI analysis for a ticket.")
	@GetMapping("/ticket/{ticketId}/analysis")
	@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
	public ResponseEntity<AiAnalysisResponse> getAnalysis(@PathVariable Long ticketId, Authentication authentication) {
		return ResponseEntity.status(HttpStatus.OK).body(aiTicketAnalysisService.getAnalysis(ticketId, authentication));
	}

	@Operation(summary = "Accept AI suggested reply", description = "Converts the saved AI suggested reply into an agent message.")
	@PostMapping("/ticket/{ticketId}/accept-suggestion")
	@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
	public ResponseEntity<ConversationResponse> acceptSuggestion(@PathVariable Long ticketId,
			Authentication authentication) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(aiTicketAnalysisService.acceptSuggestion(ticketId, authentication));
	}
}
