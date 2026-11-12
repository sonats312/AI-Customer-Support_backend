package com.ai.customer.support.service.ai;

import org.springframework.stereotype.Component;

import com.ai.customer.support.dto.AiAssistResponse;
import com.ai.customer.support.dto.AiTicketAnalysisResponse;

//@Component
public class MockAiProvider implements AiProvider {

	@Override
	public AiAssistResponse analyze(String message) {

		String lowerMessage = message.toLowerCase();

		String sentiment = detectSentiment(lowerMessage);
		String intent = detectIntent(lowerMessage);
		String category = detectCategory(lowerMessage);
		String priority = detectPriority(lowerMessage);

		String summary = createSummary(message);
		String suggestedReply = createSuggestedReply(intent, sentiment, category);

		return new AiAssistResponse(summary, sentiment, intent, category, priority, suggestedReply);
	}

	private String detectSentiment(String message) {

		if (message.contains("angry") || message.contains("frustrated") || message.contains("terrible")
				|| message.contains("worst") || message.contains("urgent")) {
			return "NEGATIVE";
		}

		if (message.contains("thank") || message.contains("great") || message.contains("happy")) {
			return "POSITIVE";
		}

		return "NEUTRAL";
	}

	private String detectIntent(String message) {

		if (message.contains("login") || message.contains("log in") || message.contains("password")) {
			return "LOGIN_ISSUE";
		}

		if (message.contains("refund") || message.contains("money back")) {
			return "REFUND_REQUEST";
		}

		if (message.contains("payment") || message.contains("charged")) {
			return "PAYMENT_ISSUE";
		}

		return "GENERAL_SUPPORT";
	}

	private String detectCategory(String message) {

		if (message.contains("login") || message.contains("log in") || message.contains("password")) {
			return "LOGIN";
		}

		if (message.contains("payment") || message.contains("charged")) {
			return "PAYMENT";
		}

		if (message.contains("refund")) {
			return "REFUND";
		}

		return "GENERAL";
	}

	private String detectPriority(String message) {

		if (message.contains("urgent") || message.contains("immediately") || message.contains("critical")) {
			return "CRITICAL";
		}

		if (message.contains("unable") || message.contains("cannot") || message.contains("can't")
				|| message.contains("failed")) {
			return "HIGH";
		}

		return "MEDIUM";
	}

	private String createSummary(String message) {

		if (message.length() > 200) {
			return message.substring(0, 200) + "...";
		}

		return message;
	}

	private String createSuggestedReply(String intent, String sentiment, String category) {

		if ("LOGIN_ISSUE".equals(intent)) {
			return "We are sorry you are experiencing a login issue. " + "Please try resetting your password using the "
					+ "Forgot Password option. If the problem continues, " + "we will investigate it further.";
		}

		if ("REFUND_REQUEST".equals(intent)) {
			return "We understand your refund request. " + "Our support team will review the transaction "
					+ "and provide you with an update shortly.";
		}

		if ("PAYMENT_ISSUE".equals(intent)) {
			return "We are sorry for the payment issue. " + "Our team will review the transaction details "
					+ "and assist you as soon as possible.";
		}

		return "Thank you for contacting our support team. "
				+ "We have received your request and will assist you shortly.";
	}

	@Override
	public AiTicketAnalysisResponse analyzeTicket(String conversation) {
		// TODO Auto-generated method stub
		return null;
	}

}
