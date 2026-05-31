package com.paymentsystem.payment.reconciliation.service;

import com.paymentsystem.common.enums.TransactionStatus;
import com.paymentsystem.payment.domain.PaymentTransaction;
import com.paymentsystem.payment.reconciliation.ReconciliationRunStatus;
import com.paymentsystem.payment.reconciliation.domain.ReconciliationRun;
import com.paymentsystem.payment.reconciliation.dto.ReconciliationMatchResult;
import com.paymentsystem.payment.reconciliation.dto.StripePaymentSnapshot;
import com.paymentsystem.payment.reconciliation.repository.ReconciliationDiscrepancyRepository;
import com.paymentsystem.payment.reconciliation.repository.ReconciliationRunRepository;
import com.paymentsystem.payment.reconciliation.repository.StripeReconciliationRecordRepository;
import com.paymentsystem.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationJobServiceTest {

	@Mock
	private ReconciliationRunRepository reconciliationRunRepository;

	@Mock
	private ReconciliationDiscrepancyRepository reconciliationDiscrepancyRepository;

	@Mock
	private StripeReconciliationRecordRepository stripeReconciliationRecordRepository;

	@Mock
	private PaymentTransactionRepository paymentTransactionRepository;

	@Mock
	private StripeReportFetcher stripeReportFetcher;

	@Mock
	private ReconciliationMatcher reconciliationMatcher;

	@InjectMocks
	private ReconciliationJobService reconciliationJobService;

	@Test
	void reconcile_skipsWhenAlreadyCompleted() {
		LocalDate runDate = LocalDate.of(2026, 5, 29);
		when(reconciliationRunRepository.findByRunDate(runDate)).thenReturn(Optional.of(
			ReconciliationRun.builder()
				.id(UUID.randomUUID())
				.runDate(runDate)
				.status(ReconciliationRunStatus.COMPLETED.name())
				.build()
		));

		reconciliationJobService.reconcile(runDate);

		verify(stripeReportFetcher, never()).fetchPaymentIntents(any(), any());
	}

	@Test
	void reconcile_persistsRunStatsWhenSuccessful() {
		LocalDate runDate = LocalDate.of(2026, 5, 29);
		UUID runId = UUID.randomUUID();
		ReconciliationRun run = ReconciliationRun.builder()
			.id(runId)
			.runDate(runDate)
			.status(ReconciliationRunStatus.RUNNING.name())
			.startedAt(Instant.now())
			.build();

		when(reconciliationRunRepository.findByRunDate(runDate)).thenReturn(Optional.empty());
		when(reconciliationRunRepository.save(any(ReconciliationRun.class))).thenReturn(run);

		StripePaymentSnapshot partner = new StripePaymentSnapshot(
			"pi_1",
			new BigDecimal("10.00"),
			"usd",
			"succeeded",
			UUID.randomUUID(),
			UUID.randomUUID(),
			Instant.parse("2026-05-29T12:00:00Z")
		);
		when(stripeReportFetcher.fetchPaymentIntents(any(), any())).thenReturn(List.of(partner));

		PaymentTransaction internal = PaymentTransaction.builder()
			.id(UUID.randomUUID())
			.walletId(UUID.randomUUID())
			.userId(partner.userId())
			.idempotencyKey("idem-1")
			.amount(new BigDecimal("10.00"))
			.status(TransactionStatus.SUCCESS.name())
			.stripePaymentIntentId("pi_1")
			.createdAt(Instant.parse("2026-05-29T12:00:00Z"))
			.updatedAt(Instant.parse("2026-05-29T12:00:00Z"))
			.build();
		when(paymentTransactionRepository.findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
			eq(TransactionStatus.SUCCESS.name()),
			any(),
			any()
		)).thenReturn(List.of(internal));

		when(reconciliationMatcher.match(eq(runId), eq(List.of(internal)), eq(List.of(partner))))
			.thenReturn(new ReconciliationMatchResult(1, List.of()));

		reconciliationJobService.reconcile(runDate);

		ArgumentCaptor<ReconciliationRun> runCaptor = ArgumentCaptor.forClass(ReconciliationRun.class);
		verify(reconciliationRunRepository, org.mockito.Mockito.atLeastOnce()).save(runCaptor.capture());
		ReconciliationRun saved = runCaptor.getAllValues().getLast();
		assertThat(saved.getStatus()).isEqualTo(ReconciliationRunStatus.COMPLETED.name());
		assertThat(saved.getInternalCount()).isEqualTo(1);
		assertThat(saved.getPartnerCount()).isEqualTo(1);
		assertThat(saved.getMatchedCount()).isEqualTo(1);
		assertThat(saved.getDiscrepancyCount()).isZero();
	}

}
