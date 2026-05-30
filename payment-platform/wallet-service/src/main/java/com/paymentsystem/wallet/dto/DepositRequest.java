package com.paymentsystem.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositRequest(
	@NotNull UUID referenceId,
	@NotNull @DecimalMin(value = "0.01", message = "Amount must be at least 0.01") BigDecimal amount,
	String description
) {
}
