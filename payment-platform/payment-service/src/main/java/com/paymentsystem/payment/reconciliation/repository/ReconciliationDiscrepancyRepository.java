package com.paymentsystem.payment.reconciliation.repository;

import com.paymentsystem.payment.reconciliation.domain.ReconciliationDiscrepancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.UUID;

public interface ReconciliationDiscrepancyRepository extends JpaRepository<ReconciliationDiscrepancy, UUID> {

	@Modifying
	void deleteByRunId(UUID runId);

}
