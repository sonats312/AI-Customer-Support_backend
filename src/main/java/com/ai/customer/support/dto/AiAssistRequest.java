package com.ai.customer.support.dto;

import jakarta.validation.constraints.NotBlank;

public record AiAssistRequest(@NotBlank(message = "Message is required") String message) {

}
