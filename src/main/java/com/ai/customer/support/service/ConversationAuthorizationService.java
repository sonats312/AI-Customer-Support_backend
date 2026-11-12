package com.ai.customer.support.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.ai.customer.support.entity.User;
import com.ai.customer.support.enums.Role;
import com.ai.customer.support.repository.ConversationRepository;
import com.ai.customer.support.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConversationAuthorizationService {

	private final ConversationRepository conversationRepository;
	private final UserRepository userRepository;
	private final TicketAuthorizationService ticketAuthorizationService;

	/**
	 * Checks whether the authenticated user can access a particular ticket
	 * conversation.
	 */
	public boolean canAccessTicketConversation(Long ticketId, Authentication authentication) {
		return ticketAuthorizationService.canAccessTicket(ticketId, authentication);
	}

	/**
	 * Checks whether the authenticated user can access a particular conversation
	 * message.
	 */
	public boolean canAccessConversation(Long conversationId, Authentication authentication) {

		var conversation = conversationRepository.findById(conversationId);

		if (conversation.isEmpty()) {
			return false;
		}
		return ticketAuthorizationService.canAccessTicket(conversation.get().getTicket().getId(), authentication);
	}

	/**
	 * Only ADMIN and AGENT can create internal notes or AI suggestions.
	 */
	public boolean canCreateInternalMessage(Authentication authentication) {

		User user = getAuthenticatedUser(authentication);
		return user.getRole() == Role.ADMIN || user.getRole() == Role.AGENT;
	}

	/**
	 * Returns the authenticated database user.
	 */
	public User getAuthenticatedUser(Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			throw new IllegalStateException("User is not authenticated");
		}
		return userRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
	}

}
