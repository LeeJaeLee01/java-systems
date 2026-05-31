package com.paymentsystem.fraud.domain;

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
@Table(name = "fraud_alerts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlert {

	@Id
	private UUID id;

	@Column(name = "wallet_id", nullable = false)
	private UUID walletId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "payment_id")
	private UUID paymentId;

	@Column(name = "alert_type", nullable = false)
	private String alertType;

	@Column(name = "transaction_count")
	private Integer transactionCount;

	@Column(nullable = false)
	private String reason;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

}
