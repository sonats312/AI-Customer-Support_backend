package com.ai.customer.support.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ai.customer.support.entity.AiAnalysis;

@Repository
public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {

	Optional<AiAnalysis> findByTicketId(Long ticketId);

	boolean existsByTicketId(Long ticketId);
}
