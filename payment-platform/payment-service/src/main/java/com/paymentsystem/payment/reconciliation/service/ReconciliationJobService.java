package com.paymentsystem.payment.reconciliation.service;

import com.paymentsystem.common.enums.TransactionStatus;
import com.paymentsystem.payment.domain.PaymentTransaction;
import com.paymentsystem.payment.reconciliation.ReconciliationRunStatus;
import com.paymentsystem.payment.reconciliation.domain.ReconciliationRun;
import com.paymentsystem.payment.reconciliation.domain.StripeReconciliationRecord;
import com.paymentsystem.payment.reconciliation.dto.ReconciliationMatchResult;
import com.paymentsystem.payment.reconciliation.dto.StripePaymentSnapshot;
import com.paymentsystem.payment.reconciliation.repository.ReconciliationDiscrepancyRepository;
import com.paymentsystem.payment.reconciliation.repository.ReconciliationRunRepository;
import com.paymentsystem.payment.reconciliation.repository.StripeReconciliationRecordRepository;
import com.paymentsystem.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationJobService {

	private final ReconciliationRunRepository reconciliationRunRepository;
	private final ReconciliationDiscrepancyRepository reconciliationDiscrepancyRepository;
	private final StripeReconciliationRecordRepository stripeReconciliationRecordRepository;
	private final PaymentTransactionRepository paymentTransactionRepository;
	private final StripeReportFetcher stripeReportFetcher;
	private final ReconciliationMatcher reconciliationMatcher;

	@Transactional
	public void reconcile(LocalDate runDate) {
		Optional<ReconciliationRun> existingRun = reconciliationRunRepository.findByRunDate(runDate);
		if (existingRun.isPresent()
			&& ReconciliationRunStatus.COMPLETED.name().equals(existingRun.get().getStatus())) {
			log.info("Reconciliation for {} already completed; skipping", runDate);
			return;
		}

		ReconciliationRun run = prepareRun(existingRun, runDate);
		Instant windowStart = runDate.atStartOfDay().toInstant(ZoneOffset.UTC);
		Instant windowEnd = runDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

		try {
			List<StripePaymentSnapshot> partnerRecords = stripeReportFetcher.fetchPaymentIntents(windowStart, windowEnd);
			persistPartnerSnapshots(run.getId(), partnerRecords);

			List<PaymentTransaction> internalLedger = paymentTransactionRepository
				.findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
					TransactionStatus.SUCCESS.name(),
					windowStart,
					windowEnd
				);

			ReconciliationMatchResult matchResult = reconciliationMatcher.match(
				run.getId(),
				internalLedger,
				partnerRecords
			);

			if (!matchResult.discrepancies().isEmpty()) {
				reconciliationDiscrepancyRepository.saveAll(matchResult.discrepancies());
			}

			run.setInternalCount(internalLedger.size());
			run.setPartnerCount(partnerRecords.size());
			run.setMatchedCount(matchResult.matchedCount());
			run.setDiscrepancyCount(matchResult.discrepancies().size());
			run.setStatus(ReconciliationRunStatus.COMPLETED.name());
			run.setCompletedAt(Instant.now());
			run.setErrorMessage(null);
			reconciliationRunRepository.save(run);

			log.info(
				"Reconciliation completed for {} — internal={} partner={} matched={} discrepancies={}",
				runDate,
				run.getInternalCount(),
				run.getPartnerCount(),
				run.getMatchedCount(),
				run.getDiscrepancyCount()
			);
		}
		catch (Exception ex) {
			run.setStatus(ReconciliationRunStatus.FAILED.name());
			run.setCompletedAt(Instant.now());
			run.setErrorMessage(ex.getMessage());
			reconciliationRunRepository.save(run);
			throw ex;
		}
	}

	private ReconciliationRun prepareRun(Optional<ReconciliationRun> existingRun, LocalDate runDate) {
		if (existingRun.isPresent()) {
			ReconciliationRun run = existingRun.get();
			stripeReconciliationRecordRepository.deleteByRunId(run.getId());
			reconciliationDiscrepancyRepository.deleteByRunId(run.getId());
			run.setStatus(ReconciliationRunStatus.RUNNING.name());
			run.setStartedAt(Instant.now());
			run.setCompletedAt(null);
			run.setErrorMessage(null);
			run.setInternalCount(0);
			run.setPartnerCount(0);
			run.setMatchedCount(0);
			run.setDiscrepancyCount(0);
			return reconciliationRunRepository.save(run);
		}

		return reconciliationRunRepository.save(ReconciliationRun.builder()
			.id(UUID.randomUUID())
			.runDate(runDate)
			.status(ReconciliationRunStatus.RUNNING.name())
			.startedAt(Instant.now())
			.build());
	}

	private void persistPartnerSnapshots(UUID runId, List<StripePaymentSnapshot> partnerRecords) {
		List<StripeReconciliationRecord> records = partnerRecords.stream()
			.map(snapshot -> StripeReconciliationRecord.builder()
				.id(UUID.randomUUID())
				.runId(runId)
				.stripePaymentIntentId(snapshot.paymentIntentId())
				.amountMinor(toMinorUnit(snapshot.amount(), snapshot.currency()))
				.currency(snapshot.currency())
				.status(snapshot.status())
				.walletId(snapshot.walletId())
				.userId(snapshot.userId())
				.stripeCreatedAt(snapshot.createdAt())
				.build())
			.toList();
		stripeReconciliationRecordRepository.saveAll(records);
	}

	private long toMinorUnit(java.math.BigDecimal amount, String currency) {
		int fractionDigits = "jpy".equalsIgnoreCase(currency) || "krw".equalsIgnoreCase(currency) ? 0 : 2;
		java.math.BigDecimal multiplier = fractionDigits == 0
			? java.math.BigDecimal.ONE
			: java.math.BigDecimal.valueOf(100);
		return amount.multiply(multiplier).longValueExact();
	}

}
