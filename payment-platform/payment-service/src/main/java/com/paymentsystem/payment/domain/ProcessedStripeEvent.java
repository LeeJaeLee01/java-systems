package com.paymentsystem.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_stripe_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedStripeEvent {

	@Id
	private UUID id;

	@Column(name = "stripe_event_id", nullable = false, unique = true)
	private String stripeEventId;

	@Column(name = "event_type", nullable = false)
	private String eventType;

	@Column(name = "processed_at", nullable = false)
	private Instant processedAt;

}
