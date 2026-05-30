package com.paymentsystem.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletTransactionResponse(
	UUID transactionId,
	UUID walletId,
	BigDecimal amount,
	BigDecimal balanceBefore,
	BigDecimal balanceAfter,
	String transactionType,
	String referenceType,
	UUID referenceId,
	Instant createdAt
) {
}
