package com.paymentsystem.payment.dto;

import com.paymentsystem.common.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
	UUID paymentId,
	UUID walletId,
	UUID userId,
	BigDecimal amount,
	TransactionStatus status,
	Instant createdAt
) {
}
