package com.ai.customer.support.service.ai;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.ai.customer.support.dto.AiAnalysisResponse;
import com.ai.customer.support.dto.AiTicketAnalysisResponse;
import com.ai.customer.support.dto.ConversationResponse;
import com.ai.customer.support.entity.AiAnalysis;
import com.ai.customer.support.entity.Conversation;
import com.ai.customer.support.entity.Ticket;
import com.ai.customer.support.entity.User;
import com.ai.customer.support.enums.MessageType;
import com.ai.customer.support.enums.Role;
import com.ai.customer.support.exception.BadRequestException;
import com.ai.customer.support.exception.ResourceNotFoundException;
import com.ai.customer.support.repository.AiAnalysisRepository;
import com.ai.customer.support.repository.ConversationRepository;
import com.ai.customer.support.repository.TicketRepository;
import com.ai.customer.support.service.ConversationAuthorizationService;
import com.ai.customer.support.service.TicketAuthorizationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiTicketAnalysisService {

	private final TicketRepository ticketRepository;
	private final ConversationRepository conversationRepository;
	private final AiAnalysisRepository aiAnalysisRepository;
	private final TicketAuthorizationService ticketAuthorizationService;
	private final AiProvider aiProvider;
	private final ConversationAuthorizationService conversationAuthorizationService;

	public AiAnalysisResponse analyzeTicket(Long ticketId, Authentication authentication) {

		log.info("Check whether the logged-in user can access this ticket, Ticket Id: {}", ticketId);
		if (!ticketAuthorizationService.canAccessTicket(ticketId, authentication)) {
			throw new AccessDeniedException("You are not authorized to analyze this ticket");
		}

		Ticket ticket = ticketRepository.findById(ticketId)
				.orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));
		log.info("Find the ticket: {}", ticket);

		List<Conversation> conversations = conversationRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
		log.info("Complete conversation history : {}", conversations);

		String conversationText = buildConversationText(ticket, conversations);
		log.info("AI input build : {}", conversationText);

		// Call AI
		AiTicketAnalysisResponse aiResponse = aiProvider.analyzeTicket(conversationText);
		log.info("AI Response : {}", aiResponse);

		AiAnalysis analysis = aiAnalysisRepository.findByTicketId(ticketId).orElseGet(AiAnalysis::new);

		analysis.setTicket(ticket);
		analysis.setSummary(aiResponse.summary());
		analysis.setSentiment(aiResponse.sentiment());
		analysis.setIntent(aiResponse.intent());
		analysis.setCategory(aiResponse.category());
		analysis.setPriority(aiResponse.priority());
		analysis.setSuggestedReply(aiResponse.suggestedReply());

		AiAnalysis savedAnalysis = aiAnalysisRepository.save(analysis);
		log.info("Saved Analysis : {}", savedAnalysis);

		return mapToResponse(savedAnalysis);
	}

	public AiAnalysisResponse getAnalysis(Long ticketId, Authentication authentication) {

		log.info("Check whether the logged-in user can access this ticket ticketId: {}", ticketId);
		if (!ticketAuthorizationService.canAccessTicket(ticketId, authentication)) {
			throw new AccessDeniedException("You are not authorized to access this ticket");
		}

		AiAnalysis analysis = aiAnalysisRepository.findByTicketId(ticketId)
				.orElseThrow(() -> new ResourceNotFoundException("AI analysis not found for ticket id: " + ticketId));
		log.info(" AI Analysis : {}", analysis);
		return mapToResponse(analysis);
	}

	public ConversationResponse acceptSuggestion(Long ticketId, Authentication authentication) {

		log.info("Check whether the logged-in user can access this ticket ticketId: {}", ticketId);
		if (!ticketAuthorizationService.canAccessTicket(ticketId, authentication)) {
			throw new AccessDeniedException("You are not authorized to access this ticket");
		}

		User agent = conversationAuthorizationService.getAuthenticatedUser(authentication);
		log.info("Authenticated User: {}", agent);

		// Only ADMIN and AGENT can accept AI suggestions
		if (agent.getRole() != Role.ADMIN && agent.getRole() != Role.AGENT) {
			throw new AccessDeniedException("Only agents and administrators can accept AI suggestions");
		}

		AiAnalysis analysis = aiAnalysisRepository.findByTicketId(ticketId)
				.orElseThrow(() -> new ResourceNotFoundException("AI analysis not found for ticket id: " + ticketId));
		log.info("Fetched saved AI Analysis: {}", analysis);

		// Make sure an AI suggestion actually exists
		if (analysis.getSuggestedReply() == null || analysis.getSuggestedReply().isBlank()) {
			throw new BadRequestException("No AI suggested reply is available for this ticket");
		}

		Ticket ticket = ticketRepository.findById(ticketId)
				.orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));
		log.info("Fetched Ticket: {}", ticket);

		// Create a customer-facing agent message
		Conversation conversation = new Conversation();
		conversation.setTicket(ticket);
		conversation.setSender(agent);
		conversation.setMessage(analysis.getSuggestedReply());
		conversation.setMessageType(MessageType.AGENT_MESSAGE);

		Conversation saved = conversationRepository.save(conversation);
		log.info("Saved Conversation : {}", saved);

		// Return the created conversation
		return new ConversationResponse(saved.getId(), saved.getTicket().getId(), saved.getSender().getId(),
				saved.getSender().getFirstName() + " " + saved.getSender().getLastName(), saved.getMessage(),
				saved.getMessageType(), saved.getCreatedAt());
	}

	private String buildConversationText(Ticket ticket, List<Conversation> conversations) {

		StringBuilder builder = new StringBuilder();
		builder.append("Ticket Number: ").append(ticket.getTicketNumber()).append("\n");
		builder.append("Subject: ").append(ticket.getSubject()).append("\n");
		builder.append("Description: ").append(ticket.getDescription()).append("\n\n");
		builder.append("Conversation:\n");

		for (Conversation conversation : conversations) {
			String senderName = conversation.getSender() != null
					? conversation.getSender().getFirstName() + " " + conversation.getSender().getLastName()
					: "Unknown";
			builder.append(senderName).append(" [").append(conversation.getMessageType()).append("]: ")
					.append(conversation.getMessage()).append("\n");
		}
		return builder.toString();
	}

	private AiAnalysisResponse mapToResponse(AiAnalysis analysis) {

		return new AiAnalysisResponse(analysis.getId(), analysis.getTicket().getId(),
				analysis.getTicket().getTicketNumber(), analysis.getSummary(), analysis.getSentiment(),
				analysis.getIntent(), analysis.getCategory(), analysis.getPriority(), analysis.getSuggestedReply(),
				analysis.getCreatedAt(), analysis.getUpdatedAt());
	}
}
