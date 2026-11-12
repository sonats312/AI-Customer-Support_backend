package com.ai.customer.support.dto;

import java.time.LocalDateTime;

public record AiAnalysisResponse(Long id, Long ticketId, String ticketNumber, String summary, String sentiment,
		String intent, String category, String priority, String suggestedReply, LocalDateTime createdAt,
		LocalDateTime updatedAt) {

}
