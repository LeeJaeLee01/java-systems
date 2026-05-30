package com.paymentsystem.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
	@NotNull UUID walletId,
	@NotNull UUID userId,
	@NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {
}
