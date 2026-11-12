package com.ai.customer.support.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

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

@ExtendWith(MockitoExtension.class)
public class ConversationServiceTest {

	@Mock
	private ConversationRepository conversationRepository;

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private ConversationAuthorizationService conversationAuthorizationService;

	@Mock
	private TicketAuthorizationService ticketAuthorizationService;

	@Mock
	private Authentication authentication;

	@Mock
	private User user;

	@Mock
	private Ticket ticket;

	@Mock
	private Conversation conversation;

	@InjectMocks
	private ConversationService conversationService;

	@Test
	void getTicketConversation_shouldRejectUnauthorizedUser() {
		Long ticketId = 10L;

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(conversationAuthorizationService.canAccessTicketConversation(ticketId, authentication)).thenReturn(false);

		assertThrows(AccessDeniedException.class,
				() -> conversationService.getTicketConversation(ticketId, authentication));

		verify(conversationRepository, never()).findByTicketIdOrderByCreatedAtAsc(ticketId);
	}

	@Test
	void getMessageById_shouldThrowExceptionWhenMessageDoesNotExist() {
		Long messageId = 999L;

		when(conversationRepository.findById(messageId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> conversationService.getMessageById(messageId, authentication));
	}

	@Test
	void deleteMessage_shouldRejectCustomer() {
		Long messageId = 1L;

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(user.getRole()).thenReturn(Role.CUSTOMER);

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(user.getRole()).thenReturn(Role.CUSTOMER);

		assertThrows(AccessDeniedException.class, () -> conversationService.deleteMessage(messageId, authentication));

		verify(conversationRepository, never()).delete(any(Conversation.class));
	}

	@Test
	void addMessage_shouldSaveCustomerMessageSuccessfully() {
		Long ticketId = 10L;

		when(user.getId()).thenReturn(1L);
		when(user.getRole()).thenReturn(Role.CUSTOMER);
		when(user.getFirstName()).thenReturn("John");
		when(user.getLastName()).thenReturn("Customer");
		when(ticket.getId()).thenReturn(10L);
		ConversationRequest request = new ConversationRequest(ticketId, 1L, "I cannot log in to my account.",
				MessageType.CUSTOMER_MESSAGE);

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(conversationAuthorizationService.canAccessTicketConversation(ticketId, authentication)).thenReturn(true);

		when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

		when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
			Conversation saved = invocation.getArgument(0);
			saved.setId(100L);
			return saved;
		});

		ConversationResponse response = conversationService.addMessage(request, authentication);

		assertNotNull(response);
		assertEquals(100L, response.id());
		assertEquals(ticketId, response.ticketId());
		assertEquals(1L, response.senderId());
		assertEquals("I cannot log in to my account.", response.message());
		assertEquals(MessageType.CUSTOMER_MESSAGE, response.messageType());

		verify(conversationRepository, times(1)).save(any(Conversation.class));
	}

	@Test
	void addMessage_shouldRejectCustomerSendingAgentMessage() {
		Long ticketId = 10L;

		ConversationRequest request = new ConversationRequest(ticketId, 1L, "This is an agent reply.",
				MessageType.AGENT_MESSAGE);

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(conversationAuthorizationService.canAccessTicketConversation(ticketId, authentication)).thenReturn(true);

		when(user.getId()).thenReturn(1L);
		when(user.getRole()).thenReturn(Role.CUSTOMER);

		assertThrows(AccessDeniedException.class, () -> conversationService.addMessage(request, authentication));

		verify(conversationRepository, never()).save(any(Conversation.class));

		verify(ticketRepository, never()).findById(ticketId);
	}

	@Test
	void getTicketConversation_shouldHideInternalMessagesFromCustomer() {
		Long ticketId = 10L;

		Conversation customerMessage = org.mockito.Mockito.mock(Conversation.class);
		Conversation agentMessage = org.mockito.Mockito.mock(Conversation.class);
		Conversation internalNote = org.mockito.Mockito.mock(Conversation.class);
		Conversation aiSuggestion = org.mockito.Mockito.mock(Conversation.class);

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(conversationAuthorizationService.canAccessTicketConversation(ticketId, authentication)).thenReturn(true);

		when(user.getRole()).thenReturn(Role.CUSTOMER);

		when(conversationRepository.findByTicketIdOrderByCreatedAtAsc(ticketId))
				.thenReturn(java.util.List.of(customerMessage, agentMessage, internalNote, aiSuggestion));

		when(customerMessage.getMessageType()).thenReturn(MessageType.CUSTOMER_MESSAGE);
		when(agentMessage.getMessageType()).thenReturn(MessageType.AGENT_MESSAGE);
		when(internalNote.getMessageType()).thenReturn(MessageType.INTERNAL_NOTE);
		when(aiSuggestion.getMessageType()).thenReturn(MessageType.AI_SUGGESTION);

		when(customerMessage.getTicket()).thenReturn(ticket);
		when(agentMessage.getTicket()).thenReturn(ticket);

		when(customerMessage.getSender()).thenReturn(user);
		when(agentMessage.getSender()).thenReturn(user);

		when(customerMessage.getMessage()).thenReturn("Customer message");
		when(agentMessage.getMessage()).thenReturn("Agent reply");

		when(customerMessage.getId()).thenReturn(101L);
		when(agentMessage.getId()).thenReturn(102L);

		when(customerMessage.getCreatedAt()).thenReturn(java.time.LocalDateTime.now());
		when(agentMessage.getCreatedAt()).thenReturn(java.time.LocalDateTime.now());

		var responses = conversationService.getTicketConversation(ticketId, authentication);

		assertEquals(2, responses.size());
		assertEquals("Customer message", responses.get(0).message());
		assertEquals("Agent reply", responses.get(1).message());

		verify(conversationRepository).findByTicketIdOrderByCreatedAtAsc(ticketId);
	}

	@Test
	void sendAgentReply_shouldSaveAgentMessageSuccessfully() {
		Long ticketId = 10L;

		SendAgentReplyRequest request = new SendAgentReplyRequest(ticketId, "Your account access has been restored.");

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(user.getId()).thenReturn(2L);
		when(user.getRole()).thenReturn(Role.AGENT);
		when(user.getFirstName()).thenReturn("John");
		when(user.getLastName()).thenReturn("Agent");
		when(ticket.getId()).thenReturn(10L);

		when(ticketAuthorizationService.canAccessTicket(ticketId, authentication)).thenReturn(true);

		when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

		when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
			Conversation saved = invocation.getArgument(0);
			saved.setId(200L);
			return saved;
		});

		ConversationResponse response = conversationService.sendAgentReply(request, authentication);

		assertNotNull(response);
		assertEquals(200L, response.id());
		assertEquals(ticketId, response.ticketId());
		assertEquals(2L, response.senderId());
		assertEquals("Your account access has been restored.", response.message());
		assertEquals(MessageType.AGENT_MESSAGE, response.messageType());

		verify(conversationRepository, times(1)).save(any(Conversation.class));
	}

	@Test
	void sendAgentReply_shouldRejectCustomer() {
		Long ticketId = 10L;

		SendAgentReplyRequest request = new SendAgentReplyRequest(ticketId, "Unauthorized agent reply");

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);
		when(user.getRole()).thenReturn(Role.CUSTOMER);
		when(user.getId()).thenReturn(1L);

		assertThrows(AccessDeniedException.class, () -> conversationService.sendAgentReply(request, authentication));

		verify(ticketRepository, never()).findById(ticketId);
		verify(conversationRepository, never()).save(any(Conversation.class));
	}

	@Test
	void sendAgentReply_shouldRejectAgentWithoutTicketAccess() {
		Long ticketId = 10L;

		SendAgentReplyRequest request = new SendAgentReplyRequest(ticketId, "Unauthorized reply");

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(user.getId()).thenReturn(2L);
		when(user.getRole()).thenReturn(Role.AGENT);

		when(ticketAuthorizationService.canAccessTicket(ticketId, authentication)).thenReturn(false);

		assertThrows(AccessDeniedException.class, () -> conversationService.sendAgentReply(request, authentication));

		verify(ticketRepository, never()).findById(ticketId);
		verify(conversationRepository, never()).save(any(Conversation.class));
	}

	@Test
	void addMessage_shouldThrowExceptionWhenTicketDoesNotExist() {
		Long ticketId = 999L;

		ConversationRequest request = new ConversationRequest(ticketId, 1L, "I need help with my account.",
				MessageType.CUSTOMER_MESSAGE);

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(conversationAuthorizationService.canAccessTicketConversation(ticketId, authentication)).thenReturn(true);

		when(user.getId()).thenReturn(1L);
		when(user.getRole()).thenReturn(Role.CUSTOMER);

		when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> conversationService.addMessage(request, authentication));

		verify(ticketRepository).findById(ticketId);
		verify(conversationRepository, never()).save(any(Conversation.class));
	}

	@Test
	void getMessageById_shouldRejectCustomerAccessToInternalNote() {
		Long messageId = 50L;

		when(conversationRepository.findById(messageId)).thenReturn(Optional.of(conversation));

		when(conversationAuthorizationService.canAccessConversation(messageId, authentication)).thenReturn(true);

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(user.getRole()).thenReturn(Role.CUSTOMER);

		when(conversation.getMessageType()).thenReturn(MessageType.INTERNAL_NOTE);

		assertThrows(AccessDeniedException.class, () -> conversationService.getMessageById(messageId, authentication));
	}

	@Test
	void deleteMessage_shouldDeleteConversationWhenAdmin() {
		Long messageId = 50L;

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(user.getId()).thenReturn(99L);
		when(user.getRole()).thenReturn(Role.ADMIN);

		when(conversationRepository.findById(messageId)).thenReturn(Optional.of(conversation));

		conversationService.deleteMessage(messageId, authentication);

		verify(conversationRepository, times(1)).findById(messageId);
		verify(conversationRepository, times(1)).delete(conversation);
	}

	@Test
	void deleteMessage_shouldThrowExceptionWhenMessageDoesNotExist() {
		Long messageId = 999L;

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(user.getId()).thenReturn(99L);
		when(user.getRole()).thenReturn(Role.ADMIN);

		when(conversationRepository.findById(messageId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> conversationService.deleteMessage(messageId, authentication));

		verify(conversationRepository).findById(messageId);
		verify(conversationRepository, never()).delete(any(Conversation.class));
	}

	@Test
	void addMessage_shouldRejectAgentCreatingInternalNoteWithoutPermission() {
		Long ticketId = 10L;

		ConversationRequest request = new ConversationRequest(ticketId, 2L, "Internal note for support staff.",
				MessageType.INTERNAL_NOTE);

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(conversationAuthorizationService.canAccessTicketConversation(ticketId, authentication)).thenReturn(true);

		when(conversationAuthorizationService.canCreateInternalMessage(authentication)).thenReturn(false);

		when(user.getId()).thenReturn(2L);
		when(user.getRole()).thenReturn(Role.AGENT);

		assertThrows(AccessDeniedException.class, () -> conversationService.addMessage(request, authentication));

		verify(conversationRepository, never()).save(any(Conversation.class));
		verify(ticketRepository, never()).findById(ticketId);
	}

	@Test
	void addMessage_shouldRejectDifferentSenderId() {
		Long ticketId = 10L;

		ConversationRequest request = new ConversationRequest(ticketId, 99L,
				"This message uses another user's identity.", MessageType.CUSTOMER_MESSAGE);

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(conversationAuthorizationService.canAccessTicketConversation(ticketId, authentication)).thenReturn(true);

		when(user.getId()).thenReturn(2L);
		when(user.getRole()).thenReturn(Role.AGENT);

		assertThrows(AccessDeniedException.class, () -> conversationService.addMessage(request, authentication));

		verify(conversationRepository, never()).save(any(Conversation.class));
		verify(ticketRepository, never()).findById(ticketId);
	}

	@Test
	void addMessage_shouldSaveInternalNoteWhenAgentIsAuthorized() {
		Long ticketId = 10L;

		ConversationRequest request = new ConversationRequest(ticketId, 2L,
				"Customer needs a follow-up from the technical team.", MessageType.INTERNAL_NOTE);

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(conversationAuthorizationService.canAccessTicketConversation(ticketId, authentication)).thenReturn(true);

		when(conversationAuthorizationService.canCreateInternalMessage(authentication)).thenReturn(true);

		when(user.getId()).thenReturn(2L);
		when(user.getRole()).thenReturn(Role.AGENT);
		when(user.getFirstName()).thenReturn("John");
		when(user.getLastName()).thenReturn("Agent");

		when(ticket.getId()).thenReturn(ticketId);
		when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

		when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
			Conversation saved = invocation.getArgument(0);
			saved.setId(300L);
			return saved;
		});

		ConversationResponse response = conversationService.addMessage(request, authentication);

		assertNotNull(response);
		assertEquals(300L, response.id());
		assertEquals(ticketId, response.ticketId());
		assertEquals(2L, response.senderId());
		assertEquals("Customer needs a follow-up from the technical team.", response.message());
		assertEquals(MessageType.INTERNAL_NOTE, response.messageType());

		verify(conversationRepository, times(1)).save(any(Conversation.class));
	}

	@Test
	void addMessage_shouldRejectAiSuggestionWithoutPermission() {
		Long ticketId = 10L;

		ConversationRequest request = new ConversationRequest(ticketId, 2L, "Suggested reply generated by AI.",
				MessageType.AI_SUGGESTION);

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(conversationAuthorizationService.canAccessTicketConversation(ticketId, authentication)).thenReturn(true);

		when(conversationAuthorizationService.canCreateInternalMessage(authentication)).thenReturn(false);

		when(user.getId()).thenReturn(2L);
		when(user.getRole()).thenReturn(Role.AGENT);

		assertThrows(AccessDeniedException.class, () -> conversationService.addMessage(request, authentication));

		verify(conversationRepository, never()).save(any(Conversation.class));
		verify(ticketRepository, never()).findById(ticketId);
	}

	@Test
	void getMessageById_shouldReturnMessageWhenAuthorized() {
		Long messageId = 50L;

		when(conversationRepository.findById(messageId)).thenReturn(Optional.of(conversation));

		when(conversationAuthorizationService.canAccessConversation(messageId, authentication)).thenReturn(true);

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(user.getId()).thenReturn(1L);
		when(user.getRole()).thenReturn(Role.CUSTOMER);
		when(user.getFirstName()).thenReturn("John");
		when(user.getLastName()).thenReturn("Customer");

		when(conversation.getId()).thenReturn(messageId);
		when(conversation.getTicket()).thenReturn(ticket);
		when(conversation.getSender()).thenReturn(user);
		when(conversation.getMessage()).thenReturn("I need help with my account.");
		when(conversation.getMessageType()).thenReturn(MessageType.CUSTOMER_MESSAGE);

		when(ticket.getId()).thenReturn(10L);

		ConversationResponse response = conversationService.getMessageById(messageId, authentication);

		assertNotNull(response);
		assertEquals(messageId, response.id());
		assertEquals(10L, response.ticketId());
		assertEquals(1L, response.senderId());
		assertEquals("John Customer", response.senderName());
		assertEquals("I need help with my account.", response.message());
		assertEquals(MessageType.CUSTOMER_MESSAGE, response.messageType());

		verify(conversationRepository).findById(messageId);
	}

	@Test
	void getTicketConversation_shouldReturnAllVisibleMessagesForAgent() {
		Long ticketId = 10L;

		Conversation customerMessage = org.mockito.Mockito.mock(Conversation.class);
		Conversation agentMessage = org.mockito.Mockito.mock(Conversation.class);
		Conversation internalNote = org.mockito.Mockito.mock(Conversation.class);

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(conversationAuthorizationService.canAccessTicketConversation(ticketId, authentication)).thenReturn(true);

		when(user.getRole()).thenReturn(Role.AGENT);

		when(conversationRepository.findByTicketIdOrderByCreatedAtAsc(ticketId))
				.thenReturn(java.util.List.of(customerMessage, agentMessage, internalNote));

		when(customerMessage.getMessageType()).thenReturn(MessageType.CUSTOMER_MESSAGE);
		when(agentMessage.getMessageType()).thenReturn(MessageType.AGENT_MESSAGE);
		when(internalNote.getMessageType()).thenReturn(MessageType.INTERNAL_NOTE);

		when(customerMessage.getTicket()).thenReturn(ticket);
		when(agentMessage.getTicket()).thenReturn(ticket);
		when(internalNote.getTicket()).thenReturn(ticket);

		when(customerMessage.getSender()).thenReturn(user);
		when(agentMessage.getSender()).thenReturn(user);
		when(internalNote.getSender()).thenReturn(user);

		when(customerMessage.getMessage()).thenReturn("Customer message");
		when(agentMessage.getMessage()).thenReturn("Agent reply");
		when(internalNote.getMessage()).thenReturn("Internal note");

		when(ticket.getId()).thenReturn(ticketId);
		when(user.getId()).thenReturn(2L);
		when(user.getFirstName()).thenReturn("John");
		when(user.getLastName()).thenReturn("Agent");

		var responses = conversationService.getTicketConversation(ticketId, authentication);

		assertEquals(3, responses.size());
		assertEquals("Customer message", responses.get(0).message());
		assertEquals("Agent reply", responses.get(1).message());
		assertEquals("Internal note", responses.get(2).message());

		verify(conversationRepository).findByTicketIdOrderByCreatedAtAsc(ticketId);
	}

	@Test
	void sendAgentReply_shouldAllowAdminToReply() {
		Long ticketId = 10L;

		SendAgentReplyRequest request = new SendAgentReplyRequest(ticketId, "We have reviewed your support request.");

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);

		when(user.getId()).thenReturn(99L);
		when(user.getRole()).thenReturn(Role.ADMIN);
		when(user.getFirstName()).thenReturn("Admin");
		when(user.getLastName()).thenReturn("User");

		when(ticketAuthorizationService.canAccessTicket(ticketId, authentication)).thenReturn(true);

		when(ticket.getId()).thenReturn(ticketId);

		when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

		when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
			Conversation saved = invocation.getArgument(0);
			saved.setId(400L);
			return saved;
		});

		ConversationResponse response = conversationService.sendAgentReply(request, authentication);

		assertNotNull(response);
		assertEquals(400L, response.id());
		assertEquals(ticketId, response.ticketId());
		assertEquals(99L, response.senderId());
		assertEquals("We have reviewed your support request.", response.message());
		assertEquals(MessageType.AGENT_MESSAGE, response.messageType());

		verify(conversationRepository, times(1)).save(any(Conversation.class));
	}

	@Test
	void getMessageById_shouldRejectUnauthorizedUser() {
		Long messageId = 50L;

		when(conversationRepository.findById(messageId)).thenReturn(Optional.of(conversation));

		when(conversationAuthorizationService.canAccessConversation(messageId, authentication)).thenReturn(false);

		assertThrows(AccessDeniedException.class, () -> conversationService.getMessageById(messageId, authentication));

		verify(conversationRepository).findById(messageId);
		verify(conversationAuthorizationService).canAccessConversation(messageId, authentication);
	}

	@Test
	void sendAgentReply_shouldThrowExceptionWhenTicketDoesNotExist() {
		SendAgentReplyRequest request = new SendAgentReplyRequest(10L, "Please try logging in again.");

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);
		when(user.getRole()).thenReturn(Role.AGENT);
		when(ticketAuthorizationService.canAccessTicket(10L, authentication)).thenReturn(true);
		when(ticketRepository.findById(10L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> conversationService.sendAgentReply(request, authentication));

		verify(ticketRepository).findById(10L);
		verify(conversationRepository, never()).save(any(Conversation.class));
	}

	@Test
	void getTicketConversation_shouldThrowExceptionWhenTicketDoesNotExist() {
		Long ticketId = 10L;

		when(conversationAuthorizationService.getAuthenticatedUser(authentication)).thenReturn(user);
		when(conversationAuthorizationService.canAccessTicketConversation(ticketId, authentication)).thenReturn(false);

		assertThrows(AccessDeniedException.class,
				() -> conversationService.getTicketConversation(ticketId, authentication));

		verify(conversationRepository, never()).findByTicketIdOrderByCreatedAtAsc(ticketId);
	}
}
