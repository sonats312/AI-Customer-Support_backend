package com.ai.customer.support.entity;

import java.time.LocalDateTime;

import com.ai.customer.support.enums.TicketPriority;
import com.ai.customer.support.enums.TicketStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Table;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String ticketNumber;

	@Column(nullable = false)
	private String subject;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TicketStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TicketPriority priority;

	private String category;

	@Column(columnDefinition = "TEXT")
	private String aiSummary;

	private String sentiment;

	private String intent;

	@Column(columnDefinition = "TEXT")
	private String suggestedReply;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_agent_id")
	private User assignedAgent;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	private LocalDateTime resolvedAt;

	@PrePersist
	protected void onCreate() {

		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();

		if (status == null) {
			status = TicketStatus.OPEN;
		}
		if (priority == null) {
			priority = TicketPriority.MEDIUM;
		}
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

}
