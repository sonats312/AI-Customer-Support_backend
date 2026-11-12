package com.ai.customer.support.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai.customer.support.dto.ConversationRequest;
import com.ai.customer.support.dto.ConversationResponse;
import com.ai.customer.support.dto.SendAgentReplyRequest;
import com.ai.customer.support.service.ConversationService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ConversationController {

	private final ConversationService conversationService;

	@PostMapping
	public ResponseEntity<ConversationResponse> addMessage(@Valid @RequestBody ConversationRequest request,
			Authentication authentication) {
		return ResponseEntity.status(HttpStatus.CREATED).body(conversationService.addMessage(request, authentication));
	}

//	Get conversation history for a ticket.
	@GetMapping("/ticket/{ticketId}")
	public ResponseEntity<List<ConversationResponse>> getTicketConversation(@PathVariable Long ticketId,
			Authentication authentication) {
		return ResponseEntity.status(HttpStatus.OK)
				.body(conversationService.getTicketConversation(ticketId, authentication));
	}

// Get one conversation message.
	@GetMapping("/{id}")
	public ResponseEntity<ConversationResponse> getMessageById(@PathVariable Long id, Authentication authentication) {
		return ResponseEntity.ok(conversationService.getMessageById(id, authentication));
	}

// Delete a conversation message. Authorization is handled inside the service.
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteMessage(@PathVariable Long id, Authentication authentication) {
		conversationService.deleteMessage(id, authentication);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/agent-reply")
	@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
	public ResponseEntity<ConversationResponse> sendAgentReply(@Valid @RequestBody SendAgentReplyRequest request,
			Authentication authentication) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(conversationService.sendAgentReply(request, authentication));
	}
}
