package com.paymentsystem.wallet.domain;

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
@Table(name = "wallet_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

	@Id
	private UUID id;

	@Column(name = "wallet_id", nullable = false)
	private UUID walletId;

	@Column(nullable = false)
	private BigDecimal amount;

	@Column(name = "balance_before", nullable = false)
	private BigDecimal balanceBefore;

	@Column(name = "balance_after", nullable = false)
	private BigDecimal balanceAfter;

	@Column(name = "transaction_type", nullable = false)
	private String transactionType;

	@Column(name = "reference_type", nullable = false)
	private String referenceType;

	@Column(name = "reference_id", nullable = false)
	private UUID referenceId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

}
