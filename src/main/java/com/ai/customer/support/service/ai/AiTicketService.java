package com.ai.customer.support.service.ai;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.ai.customer.support.dto.AiAssistResponse;
import com.ai.customer.support.dto.TicketResponse;
import com.ai.customer.support.entity.Ticket;
import com.ai.customer.support.enums.TicketPriority;
import com.ai.customer.support.exception.ResourceNotFoundException;
import com.ai.customer.support.repository.TicketRepository;
import com.ai.customer.support.service.TicketAuthorizationService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiTicketService {

	private final TicketRepository ticketRepository;
	private final AiProvider aiProvider;
	private final TicketAuthorizationService ticketAuthorizationService;

	@Transactional
	public TicketResponse analyzeTicket(Long ticketId, Authentication authentication) {

		// Check whether the authenticated user can access this ticket
		if (!ticketAuthorizationService.canAccessTicket(ticketId, authentication)) {
			throw new AccessDeniedException("You do not have permission to access this ticket");
		}

		// Find the ticket
		Ticket ticket = ticketRepository.findById(ticketId)
				.orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

		// Build the message that will be sent to Gemini
		String customerMessage = buildCustomerMessage(ticket.getSubject(), ticket.getDescription());

		// Call AI
		AiAssistResponse aiResponse = aiProvider.analyze(customerMessage);

		// Update AI fields
		ticket.setAiSummary(aiResponse.summary());
		ticket.setSentiment(aiResponse.sentiment());
		ticket.setIntent(aiResponse.intent());
		ticket.setSuggestedReply(aiResponse.suggestedReply());

		// AI becomes the source of truth for priority
		ticket.setPriority(parsePriority(aiResponse.priority()));

		// AI becomes the source of truth for category
		if (aiResponse.category() != null && !aiResponse.category().isBlank()) {
			ticket.setCategory(aiResponse.category().trim().toUpperCase());
		}

		// Save updated ticket
		Ticket updatedTicket = ticketRepository.save(ticket);

		return toResponse(updatedTicket);
	}

	private String buildCustomerMessage(String subject, String description) {
		return """
				Customer Support Ticket

				Subject:
				%s

				Description:
				%s
				""".formatted(subject, description != null ? description : "");
	}

	private TicketPriority parsePriority(String priority) {

		if (priority == null || priority.isBlank()) {
			return TicketPriority.MEDIUM;
		}
		try {
			return TicketPriority.valueOf(priority.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			return TicketPriority.MEDIUM;
		}
	}

	private TicketResponse toResponse(Ticket ticket) {

		String customerName = ticket.getCustomer() != null ? ticket.getCustomer().getName() : null;
		Long customerId = ticket.getCustomer() != null ? ticket.getCustomer().getId() : null;
		String agentName = null;
		Long agentId = null;

		if (ticket.getAssignedAgent() != null) {

			agentId = ticket.getAssignedAgent().getId();
			agentName = ticket.getAssignedAgent().getFirstName() + " " + ticket.getAssignedAgent().getLastName();
		}

		return new TicketResponse(ticket.getId(), ticket.getTicketNumber(), ticket.getSubject(),
				ticket.getDescription(), ticket.getStatus(), ticket.getPriority(), ticket.getCategory(),

				// AI fields
				ticket.getAiSummary(), ticket.getSentiment(), ticket.getIntent(), ticket.getSuggestedReply(),

				customerId, customerName, agentId, agentName, ticket.getCreatedAt(), ticket.getUpdatedAt(),
				ticket.getResolvedAt());
	}

}
