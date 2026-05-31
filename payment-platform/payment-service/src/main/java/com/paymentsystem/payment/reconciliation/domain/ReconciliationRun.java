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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_runs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRun {

	@Id
	private UUID id;

	@Column(name = "run_date", nullable = false, unique = true)
	private LocalDate runDate;

	@Column(nullable = false)
	private String status;

	@Column(name = "internal_count", nullable = false)
	private int internalCount;

	@Column(name = "partner_count", nullable = false)
	private int partnerCount;

	@Column(name = "matched_count", nullable = false)
	private int matchedCount;

	@Column(name = "discrepancy_count", nullable = false)
	private int discrepancyCount;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "error_message")
	private String errorMessage;

}
