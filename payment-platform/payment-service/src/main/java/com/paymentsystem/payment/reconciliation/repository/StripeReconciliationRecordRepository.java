package com.paymentsystem.payment.reconciliation.repository;

import com.paymentsystem.payment.reconciliation.domain.StripeReconciliationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.UUID;

public interface StripeReconciliationRecordRepository extends JpaRepository<StripeReconciliationRecord, UUID> {

	@Modifying
	void deleteByRunId(UUID runId);

}
