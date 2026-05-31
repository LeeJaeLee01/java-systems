package com.paymentsystem.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentIntentRequest(
	@NotNull UUID walletId,
	@NotNull UUID userId,
	@NotNull @DecimalMin("0.01") BigDecimal amount,
	@NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency
) {
}
