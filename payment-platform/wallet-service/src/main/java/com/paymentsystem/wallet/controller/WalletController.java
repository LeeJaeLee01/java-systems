package com.paymentsystem.wallet.controller;

import com.paymentsystem.common.dto.ApiResponse;
import com.paymentsystem.wallet.dto.CreateWalletRequest;
import com.paymentsystem.wallet.dto.WalletOperationRequest;
import com.paymentsystem.wallet.dto.WalletResponse;
import com.paymentsystem.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

	private final WalletService walletService;

	@GetMapping("/health")
	public ApiResponse<String> health() {
		return ApiResponse.ok("wallet-service is running");
	}

	@PostMapping
	public ApiResponse<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
		return ApiResponse.ok("Wallet created", walletService.createWallet(request));
	}

	@GetMapping("/users/{userId}")
	public ApiResponse<WalletResponse> getWalletByUser(@PathVariable UUID userId) {
		return ApiResponse.ok(walletService.getWalletByUserId(userId));
	}

	@PostMapping("/{walletId}/credit")
	public ApiResponse<WalletResponse> credit(
		@PathVariable UUID walletId,
		@Valid @RequestBody WalletOperationRequest request
	) {
		return ApiResponse.ok("Wallet credited", walletService.credit(walletId, request));
	}

	@PostMapping("/{walletId}/debit")
	public ApiResponse<WalletResponse> debit(
		@PathVariable UUID walletId,
		@Valid @RequestBody WalletOperationRequest request
	) {
		return ApiResponse.ok("Wallet debited", walletService.debit(walletId, request));
	}

}
