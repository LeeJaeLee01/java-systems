package com.paymentsystem.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletOperationPayload(
	UUID referenceId,
	BigDecimal amount
) {
}
