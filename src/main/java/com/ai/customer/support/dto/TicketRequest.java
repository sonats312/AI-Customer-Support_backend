package com.ai.customer.support.dto;

import com.ai.customer.support.enums.TicketPriority;

import jakarta.validation.constraints.NotBlank;

public record TicketRequest(Long customerId, @NotBlank(message = "Subject is required") String subject,
		String description, TicketPriority priority, @NotBlank(message = "Category is required") String category) {
}
