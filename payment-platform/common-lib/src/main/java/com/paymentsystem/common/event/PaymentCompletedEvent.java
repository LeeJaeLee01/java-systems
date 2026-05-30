package com.paymentsystem.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
	UUID eventId,
	UUID paymentId,
	UUID walletId,
	UUID userId,
	BigDecimal amount,
	String status,
	Instant occurredAt
) {
}
