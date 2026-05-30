package com.paymentsystem.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(
	UUID walletId,
	UUID userId,
	BigDecimal balance,
	String currency,
	String status
) {
}
