package com.ai.customer.support.dto;

import com.ai.customer.support.enums.TicketPriority;

import jakarta.validation.constraints.NotNull;

public record TicketPriorityRequest(@NotNull(message = "Priority is required") TicketPriority priority) {
}
