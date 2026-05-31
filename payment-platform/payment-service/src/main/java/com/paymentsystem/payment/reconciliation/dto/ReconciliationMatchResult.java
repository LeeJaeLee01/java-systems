package com.paymentsystem.payment.reconciliation.dto;

import com.paymentsystem.payment.reconciliation.domain.ReconciliationDiscrepancy;

import java.util.List;

public record ReconciliationMatchResult(
	int matchedCount,
	List<ReconciliationDiscrepancy> discrepancies
) {
}
