package com.paymentsystem.payment.reconciliation.repository;

import com.paymentsystem.payment.reconciliation.domain.ReconciliationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, UUID> {

	Optional<ReconciliationRun> findByRunDate(LocalDate runDate);

}
