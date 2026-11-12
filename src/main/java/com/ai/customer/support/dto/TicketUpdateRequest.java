package com.ai.customer.support.dto;

import com.ai.customer.support.enums.TicketPriority;
import com.ai.customer.support.enums.TicketStatus;

import jakarta.validation.constraints.NotNull;

public record TicketUpdateRequest(@NotNull(message = "Status is required") TicketStatus status,
		@NotNull(message = "Priority is required") TicketPriority priority) {
}
