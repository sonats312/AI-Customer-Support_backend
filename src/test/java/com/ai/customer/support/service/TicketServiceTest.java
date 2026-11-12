package com.ai.customer.support.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

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
import com.ai.customer.support.enums.Role;
import com.ai.customer.support.enums.TicketPriority;
import com.ai.customer.support.enums.TicketStatus;
import com.ai.customer.support.exception.AiAnalysisException;
import com.ai.customer.support.exception.BadRequestException;
import com.ai.customer.support.exception.ResourceNotFoundException;
import com.ai.customer.support.repository.ConversationRepository;
import com.ai.customer.support.repository.CustomerRepository;
import com.ai.customer.support.repository.TicketRepository;
import com.ai.customer.support.repository.UserRepository;
import com.ai.customer.support.service.ai.AiProvider;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private TicketAuthorizationService ticketAuthorizationService;

	@Mock
	private AiProvider aiProvider;

	@Mock
	private ConversationRepository conversationRepository;

	@Mock
	private Authentication authentication;

	@Mock
	private User user;

	@Mock
	private Customer customer;

	@Mock
	private Ticket ticket;

	@Mock
	private User agent;

	@InjectMocks
	private TicketService ticketService;

	@BeforeEach
	void setUp() {

		lenient().when(authentication.getName()).thenReturn("customer@example.com");
		lenient().when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
		lenient().when(user.getId()).thenReturn(1L);
		lenient().when(user.getEmail()).thenReturn("customer@example.com");
		lenient().when(user.getRole()).thenReturn(Role.CUSTOMER);
		lenient().when(customerRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
		lenient().when(customer.getId()).thenReturn(1L);
		lenient().when(customer.getName()).thenReturn("John Customer");
	}

	@Test
	void createTicket_shouldCreateTicketAndSaveAiAnalysis() {

		TicketRequest request = new TicketRequest(null, "Unable to login", "I cannot login to my account.",
				TicketPriority.HIGH, "LOGIN");
		AiAssistResponse aiResponse = new AiAssistResponse("Customer is unable to login.", "Frustrated",
				"resolve_login_issue", "LOGIN", "HIGH",
				"Please use the Forgot Password option to reset your password.");
		Ticket ticket = Ticket.builder().id(1L).ticketNumber("TKT-TEST123").subject(request.subject())
				.description(request.description()).priority(request.priority()).category(request.category())
				.customer(customer).build();

		when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
		when(aiProvider.analyze(anyString())).thenReturn(aiResponse);

		TicketResponse response = ticketService.createTicket(request, authentication);
		assertNotNull(response);

		verify(ticketRepository, times(2)).save(any(Ticket.class));
		verify(conversationRepository).save(any(Conversation.class));
		verify(aiProvider).analyze(anyString());
	}

	@Test
	void createTicket_shouldSaveAiCategoryWhenAiReturnsCategory() {

		// Arrange
		TicketRequest request = new TicketRequest(null, "Account problem", "I have a problem with my account.",
				TicketPriority.MEDIUM, "OTHER");

		AiAssistResponse aiResponse = new AiAssistResponse("Customer has an account-related problem.", "Neutral",
				"account_issue", "LOGIN", "MEDIUM", "Please provide more details about the account issue.");

		when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

		when(aiProvider.analyze(anyString())).thenReturn(aiResponse);

		// Act
		ticketService.createTicket(request, authentication);

		// Assert: capture both saves
		ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);

		verify(ticketRepository, times(2)).save(captor.capture());

		Ticket savedTicket = captor.getAllValues().get(1);

		assertEquals("LOGIN", savedTicket.getCategory());
		assertEquals("Customer has an account-related problem.", savedTicket.getAiSummary());
		assertEquals("Neutral", savedTicket.getSentiment());
		assertEquals("account_issue", savedTicket.getIntent());
		assertEquals("Please provide more details about the account issue.", savedTicket.getSuggestedReply());

		verify(aiProvider).analyze(anyString());
	}

	@Test
	void createTicket_shouldKeepOriginalCategoryWhenAiCategoryIsBlank() {

		// Arrange
		TicketRequest request = new TicketRequest(null, "General question", "I have a general question.",
				TicketPriority.LOW, "OTHER");

		AiAssistResponse aiResponse = new AiAssistResponse("Customer has a general question.", "Neutral",
				"general_question", "", "LOW", "Please provide more information.");

		when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

		when(aiProvider.analyze(anyString())).thenReturn(aiResponse);

		// Act
		ticketService.createTicket(request, authentication);

		// Assert
		ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);

		verify(ticketRepository, times(2)).save(captor.capture());

		Ticket savedTicket = captor.getAllValues().get(1);

		assertEquals("OTHER", savedTicket.getCategory());
		assertEquals("Customer has a general question.", savedTicket.getAiSummary());
		assertEquals("Neutral", savedTicket.getSentiment());
		assertEquals("general_question", savedTicket.getIntent());
		assertEquals("Please provide more information.", savedTicket.getSuggestedReply());

		verify(aiProvider).analyze(anyString());
	}

	@Test
	void createTicket_shouldThrowAiAnalysisExceptionWhenAiFails() {

		TicketRequest request = new TicketRequest(null, "Unable to login", "My password is not working.",
				TicketPriority.HIGH, "LOGIN");

		when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(aiProvider.analyze(anyString())).thenThrow(new AiAnalysisException("AI service unavailable"));

		AiAnalysisException exception = assertThrows(AiAnalysisException.class,
				() -> ticketService.createTicket(request, authentication));

		assertEquals("AI service unavailable", exception.getMessage());
		verify(aiProvider).analyze(anyString());
	}

	@Test
	void assignTicket_shouldAssignTicketToAgent() {

		// Arrange
		Long ticketId = 1L;
		Long agentId = 3L;

		TicketAssignRequest request = new TicketAssignRequest(agentId);

		when(ticketAuthorizationService.canModifyTicket(ticketId, authentication)).thenReturn(true);

		when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

		when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));

		when(agent.getId()).thenReturn(agentId);
		when(agent.getRole()).thenReturn(Role.AGENT);
		when(ticket.getStatus()).thenReturn(TicketStatus.OPEN);
		when(ticketRepository.save(ticket)).thenReturn(ticket);

		// Act
		TicketResponse response = ticketService.assignTicket(ticketId, request, authentication);

		// Assert
		assertNotNull(response);

		verify(ticket).setAssignedAgent(agent);
		verify(ticket).setStatus(TicketStatus.IN_PROGRESS);
		verify(ticketRepository).save(ticket);
	}

	@Test
	void assignTicket_shouldRejectCustomerAsAgent() {

		// Arrange
		Long ticketId = 1L;
		Long customerId = 5L;

		TicketAssignRequest request = new TicketAssignRequest(customerId);

		when(ticketAuthorizationService.canModifyTicket(ticketId, authentication)).thenReturn(true);

		when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

		when(userRepository.findById(customerId)).thenReturn(Optional.of(agent));

		when(agent.getRole()).thenReturn(Role.CUSTOMER);

		// Act
		BadRequestException exception = assertThrows(BadRequestException.class,
				() -> ticketService.assignTicket(ticketId, request, authentication));

		// Assert
		assertEquals("Selected user cannot be assigned tickets", exception.getMessage());

		verify(ticketRepository, times(0)).save(any(Ticket.class));
	}

	@Test
	void updateStatus_shouldUpdateTicketStatus() {

		// Arrange
		Long ticketId = 1L;

		TicketStatusRequest request = new TicketStatusRequest(TicketStatus.RESOLVED);

		when(ticketAuthorizationService.canModifyTicket(ticketId, authentication)).thenReturn(true);

		when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

		when(ticket.getStatus()).thenReturn(TicketStatus.IN_PROGRESS);
		when(ticketRepository.save(ticket)).thenReturn(ticket);

		// Act
		TicketResponse response = ticketService.updateStatus(ticketId, request, authentication);

		// Assert
		assertNotNull(response);

		verify(ticket).setStatus(TicketStatus.RESOLVED);
		verify(ticket).setResolvedAt(any(java.time.LocalDateTime.class));
		verify(ticketRepository).save(ticket);
	}

	@Test
	void updatePriority_shouldUpdateTicketPriority() {

		// Arrange
		Long ticketId = 1L;

		TicketPriorityRequest request = new TicketPriorityRequest(TicketPriority.CRITICAL);

		when(ticketAuthorizationService.canModifyTicket(ticketId, authentication)).thenReturn(true);

		when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

		when(ticket.getPriority()).thenReturn(TicketPriority.LOW);
		when(ticketRepository.save(ticket)).thenReturn(ticket);

		// Act
		TicketResponse response = ticketService.updatePriority(ticketId, request, authentication);

		// Assert
		assertNotNull(response);

		verify(ticket).setPriority(TicketPriority.CRITICAL);
		verify(ticketRepository).save(ticket);
	}

	@Test
	void updateStatus_shouldRejectUnauthorizedUser() {

		// Arrange
		Long ticketId = 1L;

		TicketStatusRequest request = new TicketStatusRequest(TicketStatus.RESOLVED);

		when(ticketAuthorizationService.canModifyTicket(ticketId, authentication)).thenReturn(false);

		// Act and Assert
		assertThrows(AccessDeniedException.class, () -> ticketService.updateStatus(ticketId, request, authentication));

		verify(ticketRepository, times(0)).findById(ticketId);
		verify(ticketRepository, times(0)).save(any(Ticket.class));
	}

	@Test
	void deleteTicket_shouldDeleteTicketWhenAuthorized() {
		Long ticketId = 1L;

		when(ticketAuthorizationService.canModifyTicket(ticketId, authentication)).thenReturn(true);
		when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

		ticketService.deleteTicket(ticketId, authentication);

		verify(ticketRepository).delete(ticket);
	}

	@Test
	void deleteTicket_shouldRejectUnauthorizedUser() {
		Long ticketId = 1L;

		when(ticketAuthorizationService.canModifyTicket(ticketId, authentication)).thenReturn(false);

		assertThrows(AccessDeniedException.class, () -> ticketService.deleteTicket(ticketId, authentication));

		verify(ticketRepository, times(0)).findById(ticketId);
		verify(ticketRepository, times(0)).delete(any(Ticket.class));
	}

	@Test
	void getTicketById_shouldReturnTicketWhenAuthorized() {
		Long ticketId = 1L;

		when(ticketAuthorizationService.canAccessTicket(ticketId, authentication)).thenReturn(true);
		when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

		TicketResponse response = ticketService.getTicketById(ticketId, authentication);

		assertNotNull(response);
		verify(ticketAuthorizationService).canAccessTicket(ticketId, authentication);
		verify(ticketRepository).findById(ticketId);
	}

	@Test
	void getTicketById_shouldRejectUnauthorizedUser() {
		Long ticketId = 1L;

		when(ticketAuthorizationService.canAccessTicket(ticketId, authentication)).thenReturn(false);

		assertThrows(AccessDeniedException.class, () -> ticketService.getTicketById(ticketId, authentication));

		verify(ticketRepository, times(0)).findById(ticketId);
	}

	@Test
	void updateStatus_shouldSetResolvedTimestampWhenTicketIsResolved() {
		Long ticketId = 1L;

		when(ticketAuthorizationService.canModifyTicket(ticketId, authentication)).thenReturn(true);
		when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
		when(ticket.getStatus()).thenReturn(TicketStatus.IN_PROGRESS);
		when(ticketRepository.save(ticket)).thenReturn(ticket);

		TicketStatusRequest request = new TicketStatusRequest(TicketStatus.RESOLVED);

		ticketService.updateStatus(ticketId, request, authentication);

		verify(ticket).setStatus(TicketStatus.RESOLVED);
		verify(ticket).setResolvedAt(any(java.time.LocalDateTime.class));
		verify(ticketRepository).save(ticket);
	}

	@Test
	void updateStatus_shouldClearResolvedTimestampWhenTicketIsReopened() {
		Long ticketId = 1L;

		when(ticketAuthorizationService.canModifyTicket(ticketId, authentication)).thenReturn(true);
		when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
		when(ticket.getStatus()).thenReturn(TicketStatus.RESOLVED);
		when(ticketRepository.save(ticket)).thenReturn(ticket);

		TicketStatusRequest request = new TicketStatusRequest(TicketStatus.IN_PROGRESS);

		ticketService.updateStatus(ticketId, request, authentication);

		verify(ticket).setStatus(TicketStatus.IN_PROGRESS);
		verify(ticket).setResolvedAt(null);
		verify(ticketRepository).save(ticket);
	}

	@Test
	void assignTicket_shouldChangeOpenTicketToInProgress() {
		Long ticketId = 1L;
		Long agentId = 2L;

		when(ticketAuthorizationService.canModifyTicket(ticketId, authentication)).thenReturn(true);
		when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
		when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));
		when(agent.getRole()).thenReturn(Role.AGENT);
		when(ticket.getStatus()).thenReturn(TicketStatus.OPEN);
		when(ticketRepository.save(ticket)).thenReturn(ticket);

		TicketAssignRequest request = new TicketAssignRequest(agentId);

		ticketService.assignTicket(ticketId, request, authentication);

		verify(ticket).setAssignedAgent(agent);
		verify(ticket).setStatus(TicketStatus.IN_PROGRESS);
		verify(ticketRepository).save(ticket);
	}

	@Test
	void assignTicket_shouldKeepInProgressStatus() {
		Long ticketId = 1L;
		Long agentId = 2L;

		when(ticketAuthorizationService.canModifyTicket(ticketId, authentication)).thenReturn(true);
		when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
		when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));
		when(agent.getRole()).thenReturn(Role.AGENT);
		when(ticket.getStatus()).thenReturn(TicketStatus.IN_PROGRESS);
		when(ticketRepository.save(ticket)).thenReturn(ticket);

		TicketAssignRequest request = new TicketAssignRequest(agentId);

		ticketService.assignTicket(ticketId, request, authentication);

		verify(ticket).setAssignedAgent(agent);
		verify(ticket, times(0)).setStatus(TicketStatus.IN_PROGRESS);
		verify(ticketRepository).save(ticket);
	}

	@Test
	void getTicketById_shouldThrowExceptionWhenTicketDoesNotExist() {
		Long ticketId = 999L;

		when(ticketAuthorizationService.canAccessTicket(ticketId, authentication)).thenReturn(true);
		when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> ticketService.getTicketById(ticketId, authentication));

		verify(ticketRepository).findById(ticketId);
	}

	@Test
	void updateStatus_shouldThrowExceptionWhenTicketDoesNotExist() {
		Long ticketId = 999L;

		TicketStatusRequest request = new TicketStatusRequest(TicketStatus.RESOLVED);

		when(ticketAuthorizationService.canModifyTicket(ticketId, authentication)).thenReturn(true);
		when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> ticketService.updateStatus(ticketId, request, authentication));

		verify(ticketRepository).findById(ticketId);
	}

	@Test
	void deleteTicket_shouldThrowExceptionWhenTicketDoesNotExist() {
		Long ticketId = 999L;

		when(ticketAuthorizationService.canModifyTicket(ticketId, authentication)).thenReturn(true);
		when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> ticketService.deleteTicket(ticketId, authentication));

		verify(ticketRepository).findById(ticketId);
		verify(ticketRepository, org.mockito.Mockito.never()).delete(any(Ticket.class));
	}

}