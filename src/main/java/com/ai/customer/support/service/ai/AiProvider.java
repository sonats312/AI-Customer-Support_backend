package com.ai.customer.support.service.ai;

import com.ai.customer.support.dto.AiAssistResponse;
import com.ai.customer.support.dto.AiTicketAnalysisResponse;

public interface AiProvider {

	AiAssistResponse analyze(String message);

	AiTicketAnalysisResponse analyzeTicket(String conversation);

}
