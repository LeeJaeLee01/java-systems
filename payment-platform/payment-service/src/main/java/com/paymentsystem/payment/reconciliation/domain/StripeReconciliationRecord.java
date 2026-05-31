package com.paymentsystem.payment.reconciliation.domain;

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
@Table(name = "stripe_reconciliation_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeReconciliationRecord {

	@Id
	private UUID id;

	@Column(name = "run_id", nullable = false)
	private UUID runId;

	@Column(name = "stripe_payment_intent_id", nullable = false)
	private String stripePaymentIntentId;

	@Column(name = "amount_minor", nullable = false)
	private long amountMinor;

	@Column(nullable = false)
	private String currency;

	@Column(nullable = false)
	private String status;

	@Column(name = "wallet_id")
	private UUID walletId;

	@Column(name = "user_id")
	private UUID userId;

	@Column(name = "stripe_created_at", nullable = false)
	private Instant stripeCreatedAt;

}
