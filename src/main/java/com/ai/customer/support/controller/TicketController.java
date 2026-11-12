package com.ai.customer.support.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai.customer.support.dto.ApiResponse;
import com.ai.customer.support.dto.TicketAssignRequest;
import com.ai.customer.support.dto.TicketPriorityRequest;
import com.ai.customer.support.dto.TicketRequest;
import com.ai.customer.support.dto.TicketResponse;
import com.ai.customer.support.dto.TicketStatusRequest;
import com.ai.customer.support.enums.TicketStatus;
import com.ai.customer.support.service.TicketService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

	private final TicketService ticketService;

	@PostMapping
	public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody TicketRequest request,
			Authentication authentication) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(request, authentication));
	}

	@GetMapping
	public ResponseEntity<List<TicketResponse>> getAccessibleTickets(Authentication authentication) {
		return ResponseEntity.status(HttpStatus.OK).body(ticketService.getAccessibleTickets(authentication));
	}

	@GetMapping("/{id}")
	public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id, Authentication authentication) {
		return ResponseEntity.status(HttpStatus.OK).body(ticketService.getTicketById(id, authentication));
	}

	@GetMapping("/number/{ticketNumber}")
	public ResponseEntity<TicketResponse> getTicketByNumber(@PathVariable String ticketNumber) {

		return ResponseEntity.status(HttpStatus.OK).body(ticketService.getTicketByNumber(ticketNumber));
	}

	@GetMapping("/customer/{customerId}")
	public ResponseEntity<List<TicketResponse>> getTicketsByCustomer(@PathVariable Long customerId) {
		return ResponseEntity.status(HttpStatus.OK).body(ticketService.getTicketsByCustomer(customerId));
	}

	@GetMapping("/agent/{agentId}")
	public ResponseEntity<List<TicketResponse>> getTicketsByAgent(@PathVariable Long agentId) {
		return ResponseEntity.status(HttpStatus.OK).body(ticketService.getTicketsByAgent(agentId));
	}

	@GetMapping("/status/{status}")
	public ResponseEntity<List<TicketResponse>> getTicketsByStatus(@PathVariable TicketStatus status) {
		return ResponseEntity.status(HttpStatus.OK).body(ticketService.getTicketsByStatus(status));
	}

	@PatchMapping("/{ticketId}/assign")
	public ResponseEntity<TicketResponse> assignTicket(@PathVariable Long ticketId,
			@Valid @RequestBody TicketAssignRequest request, Authentication authentication) {
		return ResponseEntity.status(HttpStatus.OK).body(ticketService.assignTicket(ticketId, request, authentication));
	}

	@PatchMapping("/{ticketId}/status")
	public ResponseEntity<TicketResponse> updateStatus(@PathVariable Long ticketId,
			@Valid @RequestBody TicketStatusRequest request, Authentication authentication) {
		return ResponseEntity.status(HttpStatus.OK).body(ticketService.updateStatus(ticketId, request, authentication));
	}

	@PatchMapping("/{ticketId}/priority")
	public ResponseEntity<TicketResponse> updatePriority(@PathVariable Long ticketId,
			@Valid @RequestBody TicketPriorityRequest request, Authentication authentication) {
		return ResponseEntity.status(HttpStatus.OK)
				.body(ticketService.updatePriority(ticketId, request, authentication));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse> deleteTicket(@PathVariable Long id, Authentication authentication) {
		ticketService.deleteTicket(id, authentication);
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Ticket deleted successfully"));
	}

}
