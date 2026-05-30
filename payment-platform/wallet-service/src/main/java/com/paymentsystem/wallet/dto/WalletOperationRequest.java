package com.paymentsystem.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletOperationRequest(
	@NotNull UUID referenceId,
	@NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {
}
