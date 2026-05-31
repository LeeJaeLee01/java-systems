package com.paymentsystem.payment.reconciliation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StripePaymentSnapshot(
	String paymentIntentId,
	BigDecimal amount,
	String currency,
	String status,
	UUID walletId,
	UUID userId,
	Instant createdAt
) {
}
