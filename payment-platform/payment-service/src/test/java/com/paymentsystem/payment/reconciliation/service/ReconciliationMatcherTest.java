package com.paymentsystem.payment.reconciliation.service;

import com.paymentsystem.common.enums.TransactionStatus;
import com.paymentsystem.payment.domain.PaymentTransaction;
import com.paymentsystem.payment.reconciliation.DiscrepancyType;
import com.paymentsystem.payment.reconciliation.dto.StripePaymentSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationMatcherTest {

	private final ReconciliationMatcher matcher = new ReconciliationMatcher();

	@Test
	void match_pairsByStripePaymentIntentId() {
		UUID runId = UUID.randomUUID();
		String stripeId = "pi_123";
		UUID userId = UUID.randomUUID();

		PaymentTransaction internal = payment(UUID.randomUUID(), userId, new BigDecimal("10.00"), stripeId);
		StripePaymentSnapshot partner = snapshot(stripeId, userId, new BigDecimal("10.00"), "succeeded");

		var result = matcher.match(runId, List.of(internal), List.of(partner));

		assertThat(result.matchedCount()).isEqualTo(1);
		assertThat(result.discrepancies()).isEmpty();
	}

	@Test
	void match_detectsMissingInPartner() {
		UUID runId = UUID.randomUUID();
		PaymentTransaction internal = payment(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("15.00"), null);

		var result = matcher.match(runId, List.of(internal), List.of());

		assertThat(result.matchedCount()).isZero();
		assertThat(result.discrepancies()).hasSize(1);
		assertThat(result.discrepancies().getFirst().getDiscrepancyType())
			.isEqualTo(DiscrepancyType.MISSING_IN_PARTNER.name());
	}

	@Test
	void match_detectsMissingInInternal() {
		UUID runId = UUID.randomUUID();
		StripePaymentSnapshot partner = snapshot("pi_orphan", UUID.randomUUID(), new BigDecimal("20.00"), "succeeded");

		var result = matcher.match(runId, List.of(), List.of(partner));

		assertThat(result.matchedCount()).isZero();
		assertThat(result.discrepancies()).hasSize(1);
		assertThat(result.discrepancies().getFirst().getDiscrepancyType())
			.isEqualTo(DiscrepancyType.MISSING_IN_INTERNAL.name());
	}

	@Test
	void match_detectsAmountMismatch() {
		UUID runId = UUID.randomUUID();
		String stripeId = "pi_mismatch";
		UUID userId = UUID.randomUUID();

		PaymentTransaction internal = payment(UUID.randomUUID(), userId, new BigDecimal("10.00"), stripeId);
		StripePaymentSnapshot partner = snapshot(stripeId, userId, new BigDecimal("11.00"), "succeeded");

		var result = matcher.match(runId, List.of(internal), List.of(partner));

		assertThat(result.matchedCount()).isZero();
		assertThat(result.discrepancies())
			.extracting(d -> d.getDiscrepancyType())
			.contains(DiscrepancyType.AMOUNT_MISMATCH.name());
	}

	@Test
	void match_detectsStatusMismatch() {
		UUID runId = UUID.randomUUID();
		String stripeId = "pi_status";
		UUID userId = UUID.randomUUID();

		PaymentTransaction internal = payment(UUID.randomUUID(), userId, new BigDecimal("10.00"), stripeId);
		internal.setStatus(TransactionStatus.FAILED.name());
		StripePaymentSnapshot partner = snapshot(stripeId, userId, new BigDecimal("10.00"), "succeeded");

		var result = matcher.match(runId, List.of(internal), List.of(partner));

		assertThat(result.discrepancies())
			.extracting(d -> d.getDiscrepancyType())
			.contains(DiscrepancyType.STATUS_MISMATCH.name());
	}

	private PaymentTransaction payment(UUID id, UUID userId, BigDecimal amount, String stripePaymentIntentId) {
		Instant now = Instant.now();
		return PaymentTransaction.builder()
			.id(id)
			.walletId(UUID.randomUUID())
			.userId(userId)
			.idempotencyKey("key-" + id)
			.amount(amount)
			.status(TransactionStatus.SUCCESS.name())
			.stripePaymentIntentId(stripePaymentIntentId)
			.createdAt(now)
			.updatedAt(now)
			.build();
	}

	private StripePaymentSnapshot snapshot(String id, UUID userId, BigDecimal amount, String status) {
		return new StripePaymentSnapshot(
			id,
			amount,
			"usd",
			status,
			UUID.randomUUID(),
			userId,
			Instant.now()
		);
	}

}
