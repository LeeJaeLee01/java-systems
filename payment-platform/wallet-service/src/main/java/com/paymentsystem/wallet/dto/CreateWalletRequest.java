package com.paymentsystem.wallet.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateWalletRequest(
	@NotNull UUID userId
) {
}
