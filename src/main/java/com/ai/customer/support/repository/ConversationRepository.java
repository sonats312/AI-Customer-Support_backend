package com.ai.customer.support.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ai.customer.support.entity.Conversation;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

	List<Conversation> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
