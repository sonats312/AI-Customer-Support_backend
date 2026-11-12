package com.ai.customer.support.service.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import com.ai.customer.support.dto.AiAssistResponse;
import com.ai.customer.support.dto.AiTicketAnalysisResponse;
import com.ai.customer.support.exception.AiAnalysisException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SpringAiProvider implements AiProvider {

	private final ChatClient chatClient;

	@Override
	public AiAssistResponse analyze(String message) {
		try {
			return chatClient.prompt().system("""
					You are an AI customer support assistant.

					    Analyze the customer's support ticket and return a structured
					    customer-support analysis.

					    Determine the following fields:

					    1. summary
					       Provide a short and clear summary of the customer's issue.

					    2. sentiment
					       Determine the customer's sentiment using ONLY:
					       POSITIVE, NEUTRAL, or NEGATIVE.

					       Sentiment rules:
					       - POSITIVE: The customer expresses satisfaction, appreciation,
					         happiness, or praise.
					       - NEUTRAL: The customer asks for information or assistance
					         without expressing dissatisfaction, frustration, inconvenience,
					         or a negative experience.
					       - NEGATIVE: The customer reports a problem, failure, inconvenience,
					         frustration, dissatisfaction, inability to use a service,
					         or repeated unsuccessful attempts.

					    3. intent
					       Identify the main reason for contacting customer support.

					       Examples:
					       LOGIN_ISSUE
					       PAYMENT_ISSUE
					       REFUND_REQUEST
					       ACCOUNT_ISSUE
					       TECHNICAL_ISSUE
					       ORDER_ISSUE
					       PASSWORD_RESET
					       SECURITY_ISSUE
					       OTHER

					    4. category
					       Identify the appropriate support category.

					       Examples:
					       LOGIN
					       PAYMENT
					       ACCOUNT
					       ORDER
					       REFUND
					       TECHNICAL
					       SECURITY
					       OTHER

					    5. priority
					       Determine the urgency using ONLY:
					       LOW, MEDIUM, HIGH, or CRITICAL.

					       Priority rules:
					       - LOW: General questions or issues with little immediate impact.
					       - MEDIUM: Normal support issues affecting the customer but
					         without severe impact.
					       - HIGH: Important issues preventing the customer from using
					         an important service or requiring prompt attention.
					       - CRITICAL: Security incidents, suspected fraud, account
					         compromise, major service outage, financial loss, or issues
					         with severe immediate impact.

					    6. suggestedReply
					       Provide a professional, polite, empathetic and helpful response
					       that a customer support agent can send to the customer.

					    Important rules:
					    - Return only the requested structured response.
					    - Do not add explanations outside the structured response.
					    - Do not invent information that is not present in the ticket.
					    - Use exactly the allowed values for sentiment and priority.
					    - The suggested reply must not promise actions that the support
					      team has not confirmed.
					""").user(message).call().entity(AiAssistResponse.class);
		} catch (Exception e) {
			throw new AiAnalysisException("AI analysis failed while processing the customer message.", e);
		}
	}

	@Override
	public AiTicketAnalysisResponse analyzeTicket(String conversation) {
		try {
			return chatClient.prompt().system("""
					You are an AI customer support assistant.
					...
					""").user(conversation).call().entity(AiTicketAnalysisResponse.class);
		} catch (Exception e) {

			throw new AiAnalysisException("AI ticket analysis failed.", e);
		}
	}
}
