package com.ai.customer.support.dto;

public record AiAssistResponse(String summary, String sentiment, String intent, String category, String priority,
		String suggestedReply) {

}
