package com.ai.customer.support.service.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ai.customer.support.dto.AiAssistResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiAgentAssistService {

	@Autowired
	private AiProvider aiProvider;

	public AiAssistResponse analyze(String message) {
		return aiProvider.analyze(message);
	}

}
