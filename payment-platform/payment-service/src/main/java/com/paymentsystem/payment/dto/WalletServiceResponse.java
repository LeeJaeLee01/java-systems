package com.paymentsystem.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletServiceResponse(
	UUID walletId,
	UUID userId,
	BigDecimal balance,
	String currency,
	String status
) {
}
