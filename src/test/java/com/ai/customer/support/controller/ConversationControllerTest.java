package com.ai.customer.support.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.ai.customer.support.dto.ConversationRequest;
import com.ai.customer.support.dto.ConversationResponse;
import com.ai.customer.support.dto.SendAgentReplyRequest;
import com.ai.customer.support.enums.MessageType;
import com.ai.customer.support.service.ConversationService;

@ExtendWith(MockitoExtension.class)
class ConversationControllerTest {

	@Mock
	private ConversationService conversationService;

	@Mock
	private Authentication authentication;

	@InjectMocks
	private ConversationController conversationController;

	@Test
	void addMessage_shouldReturnCreatedResponse() {
		ConversationRequest request = new ConversationRequest(10L, 1L, "I cannot log in.",
				com.ai.customer.support.enums.MessageType.CUSTOMER_MESSAGE);

		ConversationResponse expectedResponse = new ConversationResponse(100L, 10L, 1L, "John Customer",
				"I cannot log in.", com.ai.customer.support.enums.MessageType.CUSTOMER_MESSAGE,
				java.time.LocalDateTime.now());

		when(conversationService.addMessage(request, authentication)).thenReturn(expectedResponse);

		ResponseEntity<ConversationResponse> response = conversationController.addMessage(request, authentication);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(100L, response.getBody().id());
		assertEquals("I cannot log in.", response.getBody().message());

		verify(conversationService).addMessage(request, authentication);
	}

	@Test
	void getTicketConversation_shouldReturnOkResponse() {
		Long ticketId = 10L;

		List<ConversationResponse> expectedResponses = List.of(
				new ConversationResponse(100L, ticketId, 1L, "John Customer", "I cannot log in.",
						MessageType.CUSTOMER_MESSAGE, LocalDateTime.now()),
				new ConversationResponse(101L, ticketId, 2L, "John Agent", "Please reset your password.",
						MessageType.AGENT_MESSAGE, LocalDateTime.now()));

		when(conversationService.getTicketConversation(ticketId, authentication)).thenReturn(expectedResponses);

		ResponseEntity<List<ConversationResponse>> response = conversationController.getTicketConversation(ticketId,
				authentication);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(2, response.getBody().size());
		assertEquals(100L, response.getBody().get(0).id());
		assertEquals("Please reset your password.", response.getBody().get(1).message());

		verify(conversationService).getTicketConversation(ticketId, authentication);
	}

	@Test
	void getMessageById_shouldReturnOkResponse() {
		Long messageId = 100L;

		ConversationResponse expectedResponse = new ConversationResponse(messageId, 10L, 1L, "John Customer",
				"I cannot log in.", MessageType.CUSTOMER_MESSAGE, LocalDateTime.now());

		when(conversationService.getMessageById(messageId, authentication)).thenReturn(expectedResponse);

		ResponseEntity<ConversationResponse> response = conversationController.getMessageById(messageId,
				authentication);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(messageId, response.getBody().id());
		assertEquals("I cannot log in.", response.getBody().message());

		verify(conversationService).getMessageById(messageId, authentication);
	}

	@Test
	void deleteMessage_shouldReturnNoContent() {
		Long messageId = 100L;

		ResponseEntity<Void> response = conversationController.deleteMessage(messageId, authentication);

		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
		verify(conversationService).deleteMessage(messageId, authentication);
	}

	@Test
	void sendAgentReply_shouldReturnCreatedResponse() {
		SendAgentReplyRequest request = new SendAgentReplyRequest(10L, "Please reset your password and try again.");

		ConversationResponse expectedResponse = new ConversationResponse(200L, 10L, 2L, "John Agent",
				"Please reset your password and try again.", MessageType.AGENT_MESSAGE, LocalDateTime.now());

		when(conversationService.sendAgentReply(request, authentication)).thenReturn(expectedResponse);

		ResponseEntity<ConversationResponse> response = conversationController.sendAgentReply(request, authentication);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(200L, response.getBody().id());
		assertEquals("Please reset your password and try again.", response.getBody().message());

		verify(conversationService).sendAgentReply(request, authentication);
	}
}
