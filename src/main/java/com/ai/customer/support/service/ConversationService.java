package com.ai.customer.support.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.ai.customer.support.dto.ConversationRequest;
import com.ai.customer.support.dto.ConversationResponse;
import com.ai.customer.support.dto.SendAgentReplyRequest;
import com.ai.customer.support.entity.Conversation;
import com.ai.customer.support.entity.Ticket;
import com.ai.customer.support.entity.User;
import com.ai.customer.support.enums.MessageType;
import com.ai.customer.support.enums.Role;
import com.ai.customer.support.exception.ResourceNotFoundException;
import com.ai.customer.support.repository.ConversationRepository;
import com.ai.customer.support.repository.TicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

	private final ConversationRepository conversationRepository;
	private final TicketRepository ticketRepository;
	private final ConversationAuthorizationService conversationAuthorizationService;
	private final TicketAuthorizationService ticketAuthorizationService;

	public ConversationResponse addMessage(ConversationRequest request, Authentication authentication) {

		User authenticatedUser = conversationAuthorizationService.getAuthenticatedUser(authentication);

		log.info("Adding conversation message - ticketId: {}, userId: {}, role: {}, messageType: {}",
				request.ticketId(), authenticatedUser.getId(), authenticatedUser.getRole(), request.messageType());

		if (!conversationAuthorizationService.canAccessTicketConversation(request.ticketId(), authentication)) {

			log.warn("Conversation access denied - ticketId: {}, userId: {}", request.ticketId(),
					authenticatedUser.getId());

			throw new AccessDeniedException("You do not have permission to access this ticket");
		}

		// Customers and agents must use their own identity.
		if (request.senderId() != null && !request.senderId().equals(authenticatedUser.getId())) {

			log.warn("Invalid sender identity - ticketId: {}, userId: {}, requestedSenderId: {}", request.ticketId(),
					authenticatedUser.getId(), request.senderId());

			throw new AccessDeniedException("You cannot send a message as another user");
		}

		// Customers can only send CUSTOMER_MESSAGE.
		if (authenticatedUser.getRole() == Role.CUSTOMER && request.messageType() != MessageType.CUSTOMER_MESSAGE) {

			log.warn("Customer attempted restricted message type - ticketId: {}, userId: {}, messageType: {}",
					request.ticketId(), authenticatedUser.getId(), request.messageType());

			throw new AccessDeniedException("Customers can only create customer messages");
		}

		// INTERNAL_NOTE and AI_SUGGESTION are restricted.
		if ((request.messageType() == MessageType.INTERNAL_NOTE || request.messageType() == MessageType.AI_SUGGESTION)
				&& !conversationAuthorizationService.canCreateInternalMessage(authentication)) {

			log.warn("Restricted message creation denied - ticketId: {}, userId: {}, messageType: {}",
					request.ticketId(), authenticatedUser.getId(), request.messageType());

			throw new AccessDeniedException("You do not have permission to create this message type");
		}

		Ticket ticket = ticketRepository.findById(request.ticketId())
				.orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + request.ticketId()));

		Conversation conversation = Conversation.builder().ticket(ticket).sender(authenticatedUser)
				.message(request.message()).messageType(request.messageType()).build();

		Conversation savedConversation = conversationRepository.save(conversation);

		log.info(
				"Conversation message created successfully - conversationId: {}, ticketId: {}, userId: {}, messageType: {}",
				savedConversation.getId(), ticket.getId(), authenticatedUser.getId(),
				savedConversation.getMessageType());

		return mapToResponse(savedConversation);
	}

//	Get all messages belonging to a ticket.
	public List<ConversationResponse> getTicketConversation(Long ticketId, Authentication authentication) {

		User authenticatedUser = conversationAuthorizationService.getAuthenticatedUser(authentication);

		log.info("Fetching ticket conversation - ticketId: {}, userId: {}, role: {}", ticketId,
				authenticatedUser.getId(), authenticatedUser.getRole());

		if (!conversationAuthorizationService.canAccessTicketConversation(ticketId, authentication)) {

			log.warn("Ticket conversation access denied - ticketId: {}, userId: {}", ticketId,
					authenticatedUser.getId());

			throw new AccessDeniedException("You do not have permission to access this ticket");
		}

		List<Conversation> conversations = conversationRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);

		int totalMessages = conversations.size();

		// Customers must not see internal notes or AI suggestions.
		if (authenticatedUser.getRole() == Role.CUSTOMER) {
			conversations = conversations.stream()
					.filter(conversation -> conversation.getMessageType() == MessageType.CUSTOMER_MESSAGE
							|| conversation.getMessageType() == MessageType.AGENT_MESSAGE)
					.toList();
		}

		log.info("Ticket conversation fetched - ticketId: {}, totalMessages: {}, visibleMessages: {}", ticketId,
				totalMessages, conversations.size());

		return conversations.stream().map(this::mapToResponse).toList();
	}

//	Get a single conversation message.
	public ConversationResponse getMessageById(Long id, Authentication authentication) {

		log.info("Fetching conversation message - conversationId: {}", id);

		Conversation conversation = conversationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));

		if (!conversationAuthorizationService.canAccessConversation(id, authentication)) {

			log.warn("Conversation access denied - conversationId: {}", id);

			throw new AccessDeniedException("You do not have permission to access this conversation");
		}

		User authenticatedUser = conversationAuthorizationService.getAuthenticatedUser(authentication);

		log.debug("Conversation access validated - conversationId: {}, userId: {}, role: {}", id,
				authenticatedUser.getId(), authenticatedUser.getRole());

		// Hide internal information from customers.
		if (authenticatedUser.getRole() == Role.CUSTOMER && (conversation.getMessageType() == MessageType.INTERNAL_NOTE
				|| conversation.getMessageType() == MessageType.AI_SUGGESTION)) {

			log.warn("Customer attempted to access restricted message - conversationId: {}, userId: {}", id,
					authenticatedUser.getId());

			throw new AccessDeniedException("You do not have permission to access this message");
		}

		return mapToResponse(conversation);
	}

//	 Delete a conversation message.Only ADMIN can delete messages
	public void deleteMessage(Long id, Authentication authentication) {

		User authenticatedUser = conversationAuthorizationService.getAuthenticatedUser(authentication);

		log.info("Deleting conversation message - conversationId: {}, userId: {}, role: {}", id,
				authenticatedUser.getId(), authenticatedUser.getRole());

		if (authenticatedUser.getRole() != Role.ADMIN) {

			log.warn("Conversation deletion denied - conversationId: {}, userId: {}, role: {}", id,
					authenticatedUser.getId(), authenticatedUser.getRole());

			throw new AccessDeniedException("Only administrators can delete conversation messages");
		}

		Conversation conversation = conversationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));

		conversationRepository.delete(conversation);

		log.info("Conversation message deleted successfully - conversationId: {}, userId: {}", id,
				authenticatedUser.getId());
	}

	public ConversationResponse sendAgentReply(SendAgentReplyRequest request, Authentication authentication) {

		User agent = conversationAuthorizationService.getAuthenticatedUser(authentication);

		log.info("Sending agent reply - ticketId: {}, agentId: {}, role: {}", request.ticketId(), agent.getId(),
				agent.getRole());

		// Only ADMIN and AGENT can send agent replies.
		if (agent.getRole() != Role.ADMIN && agent.getRole() != Role.AGENT) {

			log.warn("Agent reply denied - ticketId: {}, userId: {}, role: {}", request.ticketId(), agent.getId(),
					agent.getRole());

			throw new AccessDeniedException("Only agents and administrators can send replies");
		}

		// Verify that the agent can access this ticket.
		if (!ticketAuthorizationService.canAccessTicket(request.ticketId(), authentication)) {

			log.warn("Agent not authorized for ticket - ticketId: {}, userId: {}", request.ticketId(), agent.getId());

			throw new AccessDeniedException("You are not authorized to reply to this ticket");
		}

		Ticket ticket = ticketRepository.findById(request.ticketId())
				.orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + request.ticketId()));

		Conversation conversation = new Conversation();

		conversation.setTicket(ticket);
		conversation.setSender(agent);
		conversation.setMessage(request.message());
		conversation.setMessageType(MessageType.AGENT_MESSAGE);

		Conversation saved = conversationRepository.save(conversation);

		log.info("Agent reply created successfully - conversationId: {}, ticketId: {}, agentId: {}", saved.getId(),
				ticket.getId(), agent.getId());

		return mapToResponse(saved);
	}

//	 Convert entity to response DTO.
	private ConversationResponse mapToResponse(Conversation conversation) {

		String senderName = null;

		if (conversation.getSender() != null) {
			senderName = conversation.getSender().getFirstName() + " " + conversation.getSender().getLastName();
		}
		return new ConversationResponse(conversation.getId(), conversation.getTicket().getId(),
				conversation.getSender() != null ? conversation.getSender().getId() : null, senderName,
				conversation.getMessage(), conversation.getMessageType(), conversation.getCreatedAt());
	}
}
