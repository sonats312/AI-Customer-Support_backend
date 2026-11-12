package com.ai.customer.support.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_analyses")
@Getter
@Setter
@NoArgsConstructor
public class AiAnalysis {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ticket_id", nullable = false, unique = true)
	private Ticket ticket;

	@Column(columnDefinition = "TEXT")
	private String summary;

	private String sentiment;

	private String intent;

	private String category;

	private String priority;

	@Column(columnDefinition = "TEXT")
	private String suggestedReply;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

}
