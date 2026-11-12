package com.ai.customer.support.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.ai.customer.support.dto.AiAssistResponse;
import com.ai.customer.support.dto.TicketAssignRequest;
import com.ai.customer.support.dto.TicketPriorityRequest;
import com.ai.customer.support.dto.TicketRequest;
import com.ai.customer.support.dto.TicketResponse;
import com.ai.customer.support.dto.TicketStatusRequest;
import com.ai.customer.support.entity.Conversation;
import com.ai.customer.support.entity.Customer;
import com.ai.customer.support.entity.Ticket;
import com.ai.customer.support.entity.User;
import com.ai.customer.support.enums.MessageType;
import com.ai.customer.support.enums.Role;
import com.ai.customer.support.enums.TicketPriority;
import com.ai.customer.support.enums.TicketStatus;
import com.ai.customer.support.exception.BadRequestException;
import com.ai.customer.support.exception.ResourceNotFoundException;
import com.ai.customer.support.repository.ConversationRepository;
import com.ai.customer.support.repository.CustomerRepository;
import com.ai.customer.support.repository.TicketRepository;
import com.ai.customer.support.repository.UserRepository;
import com.ai.customer.support.service.ai.AiProvider;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

	private final TicketRepository ticketRepository;
	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;
	private final TicketAuthorizationService ticketAuthorizationService;
	private final AiProvider aiProvider;
	private final ConversationRepository conversationRepository;

	@Transactional
	public TicketResponse createTicket(TicketRequest request, Authentication authentication) {

		User user = getAuthenticatedUser(authentication);
		log.info("Creating ticket - userId: {}, role: {}", user.getId(), user.getRole());
		Customer customer;

		if (user.getRole() == Role.CUSTOMER) {
			customer = customerRepository.findByEmail(user.getEmail())
					.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
		} else {
			customer = customerRepository.findById(request.customerId())
					.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
		}

		Ticket ticket = Ticket.builder().ticketNumber(generateTicketNumber()).subject(request.subject())
				.description(request.description())
				.priority(request.priority() != null ? request.priority() : TicketPriority.MEDIUM)
				.status(TicketStatus.OPEN).category(request.category()).customer(customer).build();

		Ticket savedTicket = ticketRepository.save(ticket);

		log.info("Ticket created successfully - ticketId: {}, ticketNumber: {}, customerId: {}, priority: {}",
				savedTicket.getId(), savedTicket.getTicketNumber(), customer.getId(), savedTicket.getPriority());

		// Create initial customer conversation message
		Conversation conversation = Conversation.builder().ticket(savedTicket).sender(user)
				.message(request.description()).messageType(MessageType.CUSTOMER_MESSAGE).build();

		conversationRepository.save(conversation);

		log.debug("Initial customer conversation created - ticketId: {}", savedTicket.getId());

		// Ask AI to analyze the ticket

		String customerMessage = buildCustomerMessage(request.subject(), request.description());

		log.info("Starting AI analysis - ticketId: {}, ticketNumber: {}", savedTicket.getId(),
				savedTicket.getTicketNumber());

		AiAssistResponse aiResponse = aiProvider.analyze(customerMessage);

		log.info("AI analysis completed - ticketId: {}, category: {}, sentiment: {}, intent: {}", savedTicket.getId(),
				aiResponse.category(), aiResponse.sentiment(), aiResponse.intent());

		savedTicket.setAiSummary(aiResponse.summary());
		savedTicket.setSentiment(aiResponse.sentiment());
		savedTicket.setIntent(aiResponse.intent());
		savedTicket.setSuggestedReply(aiResponse.suggestedReply());

		if (aiResponse.category() != null && !aiResponse.category().isBlank()) {

			savedTicket.setCategory(aiResponse.category());
		}

		savedTicket = ticketRepository.save(savedTicket);

		return toResponse(savedTicket);
	}

	// BUILD CUSTOMER MESSAGE FOR AI
	private String buildCustomerMessage(String subject, String description) {
		return """
				Customer Support Ticket
				Subject:
				%s
				Description:
				%s
				""".formatted(subject, description != null ? description : "");
	}

	public List<TicketResponse> getAccessibleTickets(Authentication authentication) {

		User user = getAuthenticatedUser(authentication);

		log.info("Fetching accessible tickets - userId: {}, role: {}", user.getId(), user.getRole());

		List<Ticket> tickets;
		if (user.getRole() == Role.ADMIN) {
			tickets = ticketRepository.findAll();
		} else if (user.getRole() == Role.AGENT) {
			tickets = ticketRepository.findByAssignedAgentId(user.getId());
		} else if (user.getRole() == Role.CUSTOMER) {
			tickets = ticketRepository.findByCustomerId(user.getId());
		} else {
			tickets = List.of();
		}

		log.info("Tickets fetched successfully - userId: {}, count: {}", user.getId(), tickets.size());
		return tickets.stream().map(this::toResponse).toList();
	}

	// AUTHENTICATED USER
	private User getAuthenticatedUser(Authentication authentication) {
		return userRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
	}

	public TicketResponse getTicketById(Long id, Authentication authentication) {

		if (!ticketAuthorizationService.canAccessTicket(id, authentication)) {
			throw new AccessDeniedException("You do not have permission to access this ticket");
		}

		Ticket ticket = findTicket(id);
		return toResponse(ticket);
	}

	public TicketResponse getTicketByNumber(String ticketNumber) {

		Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
				.orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketNumber));
		return toResponse(ticket);
	}

	public List<TicketResponse> getTicketsByCustomer(Long customerId) {

		if (!customerRepository.existsById(customerId)) {
			throw new ResourceNotFoundException("Customer not found with id: " + customerId);
		}
		return ticketRepository.findByCustomerId(customerId).stream().map(this::toResponse).toList();
	}

	public List<TicketResponse> getTicketsByAgent(Long agentId) {

		if (!userRepository.existsById(agentId)) {
			throw new ResourceNotFoundException("User not found with id: " + agentId);
		}
		return ticketRepository.findByAssignedAgentId(agentId).stream().map(this::toResponse).toList();
	}

	public List<TicketResponse> getTicketsByStatus(TicketStatus status) {
		return ticketRepository.findByStatus(status).stream().map(this::toResponse).toList();
	}

	public TicketResponse assignTicket(Long ticketId, TicketAssignRequest request, Authentication authentication) {

		if (!ticketAuthorizationService.canModifyTicket(ticketId, authentication)) {
			throw new AccessDeniedException("You do not have permission to update this ticket");
		}

		Ticket ticket = findTicket(ticketId);

		log.info("Assigning ticket - ticketId: {}, requestedAgentId: {}", ticketId, request.agentId());

		// Unassign the current agent when agentId is null
		if (request.agentId() == null) {
			ticket.setAssignedAgent(null);

			if (ticket.getStatus() == TicketStatus.IN_PROGRESS) {
				ticket.setStatus(TicketStatus.OPEN);
			}

			Ticket updatedTicket = ticketRepository.save(ticket);

			log.info("Ticket unassigned successfully - ticketId: {}, status: {}", ticketId, updatedTicket.getStatus());

			return toResponse(updatedTicket);
		}

		// Existing assignment logic
		User agent = userRepository.findById(request.agentId())
				.orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + request.agentId()));

		if (agent.getRole() != Role.AGENT && agent.getRole() != Role.ADMIN) {
			throw new BadRequestException("Selected user cannot be assigned tickets");
		}

		ticket.setAssignedAgent(agent);

		if (ticket.getStatus() == TicketStatus.OPEN) {
			ticket.setStatus(TicketStatus.IN_PROGRESS);
		}

		Ticket updatedTicket = ticketRepository.save(ticket);

		log.info("Ticket assigned successfully - ticketId: {}, agentId: {}, status: {}", ticketId, agent.getId(),
				updatedTicket.getStatus());

		return toResponse(updatedTicket);
	}

	public TicketResponse updateStatus(Long ticketId, TicketStatusRequest request, Authentication authentication) {

		if (!ticketAuthorizationService.canModifyTicket(ticketId, authentication)) {
			throw new AccessDeniedException("You do not have permission to update this ticket");
		}

		Ticket ticket = findTicket(ticketId);

		TicketStatus oldStatus = ticket.getStatus();
		TicketStatus newStatus = request.status();

		log.info("Updating ticket status - ticketId: {}, oldStatus: {}, newStatus: {}", ticketId, oldStatus, newStatus);

		ticket.setStatus(newStatus);

		if (newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CLOSED) {
			ticket.setResolvedAt(LocalDateTime.now());
		} else {
			ticket.setResolvedAt(null);
		}

		Ticket updatedTicket = ticketRepository.save(ticket);

		log.info("Ticket status updated successfully - ticketId: {}, status: {}", ticketId, updatedTicket.getStatus());

		return toResponse(updatedTicket);
	}

	public TicketResponse updatePriority(Long ticketId, TicketPriorityRequest request, Authentication authentication) {

		if (!ticketAuthorizationService.canModifyTicket(ticketId, authentication)) {
			throw new AccessDeniedException("You do not have permission to update this ticket");
		}

		Ticket ticket = findTicket(ticketId);

		TicketPriority oldPriority = ticket.getPriority();
		TicketPriority newPriority = request.priority();

		log.info("Updating ticket priority - ticketId: {}, oldPriority: {}, newPriority: {}", ticketId, oldPriority,
				newPriority);

		ticket.setPriority(newPriority);

		Ticket updatedTicket = ticketRepository.save(ticket);

		log.info("Ticket priority updated successfully - ticketId: {}, priority: {}", ticketId,
				updatedTicket.getPriority());

		return toResponse(updatedTicket);
	}

	public void deleteTicket(Long ticketId, Authentication authentication) {

		if (!ticketAuthorizationService.canModifyTicket(ticketId, authentication)) {
			throw new AccessDeniedException("You do not have permission to update this ticket");
		}

		Ticket ticket = findTicket(ticketId);

		log.info("Deleting ticket - ticketId: {}, ticketNumber: {}", ticket.getId(), ticket.getTicketNumber());

		ticketRepository.delete(ticket);

		log.info("Ticket deleted successfully - ticketId: {}, ticketNumber: {}", ticket.getId(),
				ticket.getTicketNumber());
	}

	private Ticket findTicket(Long id) {
		return ticketRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
	}

	private String generateTicketNumber() {
		return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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

		return new TicketResponse(

				ticket.getId(), ticket.getTicketNumber(), ticket.getSubject(), ticket.getDescription(),
				ticket.getStatus(), ticket.getPriority(), ticket.getCategory(), ticket.getAiSummary(),
				ticket.getSentiment(), ticket.getIntent(), ticket.getSuggestedReply(), customerId, customerName,
				agentId, agentName, ticket.getCreatedAt(), ticket.getUpdatedAt(), ticket.getResolvedAt());
	}
}
