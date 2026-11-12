package com.ai.customer.support.dto;

import com.ai.customer.support.enums.TicketStatus;

import jakarta.validation.constraints.NotNull;

public record TicketStatusRequest(@NotNull(message = "Status is required") TicketStatus status) {
}
