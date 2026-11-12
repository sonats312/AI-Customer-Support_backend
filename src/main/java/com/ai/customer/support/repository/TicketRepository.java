package com.ai.customer.support.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ai.customer.support.entity.Ticket;
import com.ai.customer.support.enums.TicketStatus;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

	Optional<Ticket> findByTicketNumber(String ticketNumber);

	List<Ticket> findByStatus(TicketStatus status);

	List<Ticket> findByCustomerId(Long customerId);

	List<Ticket> findByAssignedAgentId(Long agentId);

	boolean existsByIdAndCustomerId(Long ticketId, Long customerId);

	boolean existsByIdAndAssignedAgentId(Long ticketId, Long agentId);

}
