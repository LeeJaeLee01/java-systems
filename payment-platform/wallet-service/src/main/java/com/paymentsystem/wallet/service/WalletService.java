package com.paymentsystem.wallet.service;

import com.paymentsystem.wallet.domain.Wallet;
import com.paymentsystem.wallet.domain.WalletTransaction;
import com.paymentsystem.wallet.dto.CreateWalletRequest;
import com.paymentsystem.wallet.dto.WalletOperationRequest;
import com.paymentsystem.wallet.dto.WalletResponse;
import com.paymentsystem.wallet.repository.WalletRepository;
import com.paymentsystem.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

	private final WalletRepository walletRepository;
	private final WalletTransactionRepository walletTransactionRepository;

	@Transactional
	public WalletResponse createWallet(CreateWalletRequest request) {
		walletRepository.findByUserId(request.userId()).ifPresent(wallet -> {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Wallet already exists for user");
		});

		Instant now = Instant.now();
		Wallet wallet = Wallet.builder()
			.id(UUID.randomUUID())
			.userId(request.userId())
			.balance(BigDecimal.ZERO)
			.currency("USD")
			.status("ACTIVE")
			.createdAt(now)
			.updatedAt(now)
			.build();

		return toResponse(walletRepository.save(wallet));
	}

	@Transactional(readOnly = true)
	public WalletResponse getWalletByUserId(UUID userId) {
		return walletRepository.findByUserId(userId)
			.map(this::toResponse)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
	}

	@Transactional
	public WalletResponse credit(UUID walletId, WalletOperationRequest request) {
		Wallet wallet = walletRepository.findByIdForUpdate(walletId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

		BigDecimal balanceBefore = wallet.getBalance();
		BigDecimal balanceAfter = balanceBefore.add(request.amount());
		wallet.setBalance(balanceAfter);
		wallet.setUpdatedAt(Instant.now());

		saveLedger(wallet, request, balanceBefore, balanceAfter, "CREDIT");
		return toResponse(wallet);
	}

	@Transactional
	public WalletResponse debit(UUID walletId, WalletOperationRequest request) {
		Wallet wallet = walletRepository.findByIdForUpdate(walletId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

		BigDecimal balanceBefore = wallet.getBalance();
		if (balanceBefore.compareTo(request.amount()) < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
		}

		BigDecimal balanceAfter = balanceBefore.subtract(request.amount());
		wallet.setBalance(balanceAfter);
		wallet.setUpdatedAt(Instant.now());

		saveLedger(wallet, request, balanceBefore, balanceAfter, "DEBIT");
		return toResponse(wallet);
	}

	private void saveLedger(
		Wallet wallet,
		WalletOperationRequest request,
		BigDecimal balanceBefore,
		BigDecimal balanceAfter,
		String transactionType
	) {
		walletTransactionRepository.save(WalletTransaction.builder()
			.id(UUID.randomUUID())
			.walletId(wallet.getId())
			.amount(request.amount())
			.balanceBefore(balanceBefore)
			.balanceAfter(balanceAfter)
			.transactionType(transactionType)
			.referenceType("PAYMENT")
			.referenceId(request.referenceId())
			.createdAt(Instant.now())
			.build());
	}

	private WalletResponse toResponse(Wallet wallet) {
		return new WalletResponse(
			wallet.getId(),
			wallet.getUserId(),
			wallet.getBalance(),
			wallet.getCurrency(),
			wallet.getStatus()
		);
	}

}
