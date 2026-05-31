package com.paymentsystem.payment.reconciliation.service;

import com.paymentsystem.common.enums.TransactionStatus;
import com.paymentsystem.payment.domain.PaymentTransaction;
import com.paymentsystem.payment.reconciliation.DiscrepancyType;
import com.paymentsystem.payment.reconciliation.domain.ReconciliationDiscrepancy;
import com.paymentsystem.payment.reconciliation.dto.ReconciliationMatchResult;
import com.paymentsystem.payment.reconciliation.dto.StripePaymentSnapshot;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ReconciliationMatcher {

	private static final String STRIPE_SUCCEEDED = "succeeded";

	public ReconciliationMatchResult match(
		UUID runId,
		List<PaymentTransaction> internalLedger,
		List<StripePaymentSnapshot> partnerRecords
	) {
		Map<String, StripePaymentSnapshot> partnerById = new HashMap<>();
		for (StripePaymentSnapshot snapshot : partnerRecords) {
			partnerById.put(snapshot.paymentIntentId(), snapshot);
		}

		Set<UUID> matchedInternalIds = new HashSet<>();
		Set<String> matchedPartnerIds = new HashSet<>();
		List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();
		int matchedCount = 0;

		for (PaymentTransaction payment : internalLedger) {
			StripePaymentSnapshot partner = resolvePartnerMatch(payment, partnerById, matchedPartnerIds);
			if (partner == null) {
				continue;
			}

			matchedInternalIds.add(payment.getId());
			matchedPartnerIds.add(partner.paymentIntentId());

			List<ReconciliationDiscrepancy> pairDiscrepancies = comparePair(runId, payment, partner);
			if (pairDiscrepancies.isEmpty()) {
				matchedCount++;
			}
			else {
				discrepancies.addAll(pairDiscrepancies);
			}
		}

		Instant detectedAt = Instant.now();
		for (PaymentTransaction payment : internalLedger) {
			if (matchedInternalIds.contains(payment.getId())) {
				continue;
			}
			if (!TransactionStatus.SUCCESS.name().equals(payment.getStatus())) {
				continue;
			}
			discrepancies.add(ReconciliationDiscrepancy.builder()
				.id(UUID.randomUUID())
				.runId(runId)
				.discrepancyType(DiscrepancyType.MISSING_IN_PARTNER.name())
				.internalPaymentId(payment.getId())
				.internalAmount(payment.getAmount())
				.internalStatus(payment.getStatus())
				.detectedAt(detectedAt)
				.build());
		}

		for (StripePaymentSnapshot partner : partnerRecords) {
			if (matchedPartnerIds.contains(partner.paymentIntentId())) {
				continue;
			}
			if (!STRIPE_SUCCEEDED.equals(partner.status())) {
				continue;
			}
			discrepancies.add(ReconciliationDiscrepancy.builder()
				.id(UUID.randomUUID())
				.runId(runId)
				.discrepancyType(DiscrepancyType.MISSING_IN_INTERNAL.name())
				.stripePaymentIntentId(partner.paymentIntentId())
				.partnerAmount(partner.amount())
				.partnerStatus(partner.status())
				.detectedAt(detectedAt)
				.build());
		}

		return new ReconciliationMatchResult(matchedCount, discrepancies);
	}

	private StripePaymentSnapshot resolvePartnerMatch(
		PaymentTransaction payment,
		Map<String, StripePaymentSnapshot> partnerById,
		Set<String> alreadyMatchedPartnerIds
	) {
		if (payment.getStripePaymentIntentId() != null) {
			StripePaymentSnapshot byId = partnerById.get(payment.getStripePaymentIntentId());
			if (byId != null && !alreadyMatchedPartnerIds.contains(byId.paymentIntentId())) {
				return byId;
			}
		}

		for (StripePaymentSnapshot candidate : partnerById.values()) {
			if (alreadyMatchedPartnerIds.contains(candidate.paymentIntentId())) {
				continue;
			}
			if (payment.getUserId().equals(candidate.userId())
				&& payment.getAmount().compareTo(candidate.amount()) == 0) {
				return candidate;
			}
		}
		return null;
	}

	private List<ReconciliationDiscrepancy> comparePair(
		UUID runId,
		PaymentTransaction payment,
		StripePaymentSnapshot partner
	) {
		List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();
		Instant detectedAt = Instant.now();

		if (payment.getAmount().compareTo(partner.amount()) != 0) {
			discrepancies.add(buildDiscrepancy(
				runId,
				DiscrepancyType.AMOUNT_MISMATCH,
				payment,
				partner,
				detectedAt
			));
		}

		boolean internalSuccess = TransactionStatus.SUCCESS.name().equals(payment.getStatus());
		boolean partnerSuccess = STRIPE_SUCCEEDED.equals(partner.status());
		if (internalSuccess != partnerSuccess) {
			discrepancies.add(buildDiscrepancy(
				runId,
				DiscrepancyType.STATUS_MISMATCH,
				payment,
				partner,
				detectedAt
			));
		}

		return discrepancies;
	}

	private ReconciliationDiscrepancy buildDiscrepancy(
		UUID runId,
		DiscrepancyType type,
		PaymentTransaction payment,
		StripePaymentSnapshot partner,
		Instant detectedAt
	) {
		return ReconciliationDiscrepancy.builder()
			.id(UUID.randomUUID())
			.runId(runId)
			.discrepancyType(type.name())
			.internalPaymentId(payment.getId())
			.stripePaymentIntentId(partner.paymentIntentId())
			.internalAmount(payment.getAmount())
			.partnerAmount(partner.amount())
			.internalStatus(payment.getStatus())
			.partnerStatus(partner.status())
			.detectedAt(detectedAt)
			.build();
	}

}
