package com.ai.customer.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendAgentReplyRequest(@NotNull(message = "Ticket ID is required") Long ticketId,
		@NotBlank(message = "Message is required") String message) {
}
