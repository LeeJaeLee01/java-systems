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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_discrepancies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationDiscrepancy {

	@Id
	private UUID id;

	@Column(name = "run_id", nullable = false)
	private UUID runId;

	@Column(name = "discrepancy_type", nullable = false)
	private String discrepancyType;

	@Column(name = "internal_payment_id")
	private UUID internalPaymentId;

	@Column(name = "stripe_payment_intent_id")
	private String stripePaymentIntentId;

	@Column(name = "internal_amount")
	private BigDecimal internalAmount;

	@Column(name = "partner_amount")
	private BigDecimal partnerAmount;

	@Column(name = "internal_status")
	private String internalStatus;

	@Column(name = "partner_status")
	private String partnerStatus;

	@Column(name = "detected_at", nullable = false)
	private Instant detectedAt;

}
