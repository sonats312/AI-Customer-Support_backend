package com.ai.customer.support.dto;

import java.time.LocalDateTime;

import com.ai.customer.support.enums.MessageType;

public record ConversationResponse(Long id, Long ticketId, Long senderId, String senderName, String message,
		MessageType messageType, LocalDateTime createdAt) {
}
