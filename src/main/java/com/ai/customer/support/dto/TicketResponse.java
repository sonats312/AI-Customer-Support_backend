package com.ai.customer.support.dto;

import java.time.LocalDateTime;

import com.ai.customer.support.enums.TicketPriority;
import com.ai.customer.support.enums.TicketStatus;

public record TicketResponse(Long id, String ticketNumber, String subject, String description, TicketStatus status,
		TicketPriority priority, String category, String aiSummary, String sentiment, String intent,
		String suggestedReply, Long customerId, String customerName, Long assignedAgentId, String assignedAgentName,
		LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime resolvedAt) {
}
