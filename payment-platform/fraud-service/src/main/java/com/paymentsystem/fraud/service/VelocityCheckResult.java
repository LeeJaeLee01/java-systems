package com.paymentsystem.fraud.service;

import java.util.UUID;

public record VelocityCheckResult(
	UUID userId,
	long transactionCount,
	long windowSeconds,
	long maxTransactions,
	boolean exceeded
) {
}
