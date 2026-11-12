package com.ai.customer.support.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.ai.customer.support.entity.User;
import com.ai.customer.support.enums.Role;
import com.ai.customer.support.exception.BadRequestException;
import com.ai.customer.support.repository.TicketRepository;
import com.ai.customer.support.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketAuthorizationService {

	private final TicketRepository ticketRepository;
	private final UserRepository userRepository;

	public boolean canAccessTicket(Long ticketId, Authentication authentication) {

		User user = getAuthenticatedUser(authentication);
		// ADMIN can access everything
		if (user.getRole() == Role.ADMIN) {
			return true;
		}
		// AGENT can access tickets assigned to them
		if (user.getRole() == Role.AGENT) {
			return ticketRepository.existsByIdAndAssignedAgentId(ticketId, user.getId());
		}
		// CUSTOMER can access only their own tickets
		if (user.getRole() == Role.CUSTOMER) {
			return ticketRepository.existsByIdAndCustomerId(ticketId, user.getId());
		}
		return false;
	}

	public boolean canModifyTicket(Long ticketId, Authentication authentication) {

		User user = getAuthenticatedUser(authentication);
		if (user.getRole() == Role.ADMIN) {
			return true;
		}
		if (user.getRole() == Role.AGENT) {
			return ticketRepository.existsByIdAndAssignedAgentId(ticketId, user.getId());
		}
		return false;
	}

	private User getAuthenticatedUser(Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BadRequestException("User is not authenticated");
		}
		String email = authentication.getName();

		return userRepository.findByEmail(email)
				.orElseThrow(() -> new BadRequestException("Authenticated user not found"));
	}

}
