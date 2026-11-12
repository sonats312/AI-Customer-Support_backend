package com.ai.customer.support.dto;

import com.ai.customer.support.enums.MessageType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConversationRequest(@NotNull(message = "Ticket ID is required") Long ticketId, Long senderId,
		@NotBlank(message = "Message cannot be empty") String message,
		@NotNull(message = "Message type is required") MessageType messageType) {
}
