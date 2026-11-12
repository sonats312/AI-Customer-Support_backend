package com.ai.customer.support.exception;

public class AiAnalysisException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AiAnalysisException(String message) {
		super(message);
	}

	public AiAnalysisException(String message, Throwable cause) {
		super(message, cause);
	}
}
