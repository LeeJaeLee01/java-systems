package com.paymentsystem.wallet.service;

import com.paymentsystem.wallet.dto.CreateWalletRequest;
import com.paymentsystem.wallet.dto.DepositRequest;
import com.paymentsystem.wallet.dto.WalletOperationRequest;
import com.paymentsystem.wallet.dto.WalletResponse;
import com.paymentsystem.wallet.dto.WalletTransactionResponse;
import com.paymentsystem.wallet.dto.WithdrawRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

	private final WalletBalanceService walletBalanceService;
	private final WalletDistributedLockService walletDistributedLockService;
	private final RedisWalletBalanceCache walletBalanceCache;

	public WalletResponse createWallet(CreateWalletRequest request) {
		return walletBalanceService.createWallet(request);
	}

	public WalletResponse getWalletByUserId(UUID userId) {
		return walletBalanceService.getWalletByUserId(userId);
	}

	public WalletResponse getWalletById(UUID walletId) {
		return walletBalanceService.getWalletById(walletId);
	}

	public List<WalletTransactionResponse> getTransactions(UUID walletId) {
		return walletBalanceService.getTransactions(walletId);
	}

	public WalletResponse deposit(UUID walletId, DepositRequest request) {
		return walletDistributedLockService.executeWithWalletLock(walletId, () -> {
			walletBalanceCache.ensureLoaded(walletId, () -> walletBalanceService.loadBalance(walletId));
			return walletBalanceService.deposit(walletId, request);
		});
	}

	public WalletResponse withdraw(UUID walletId, WithdrawRequest request) {
		return executeDebitWithLock(
			walletId,
			request.amount(),
			() -> walletBalanceService.withdraw(walletId, request)
		);
	}

	public WalletResponse credit(UUID walletId, WalletOperationRequest request) {
		return walletDistributedLockService.executeWithWalletLock(walletId, () -> {
			walletBalanceCache.ensureLoaded(walletId, () -> walletBalanceService.loadBalance(walletId));
			return walletBalanceService.credit(walletId, request);
		});
	}

	public WalletResponse debit(UUID walletId, WalletOperationRequest request) {
		return executeDebitWithLock(
			walletId,
			request.amount(),
			() -> walletBalanceService.debit(walletId, request)
		);
	}

	private WalletResponse executeDebitWithLock(
		UUID walletId,
		BigDecimal amount,
		java.util.function.Supplier<WalletResponse> debitAction
	) {
		return walletDistributedLockService.executeWithWalletLock(walletId, () -> {
			walletBalanceCache.ensureLoaded(walletId, () -> walletBalanceService.loadBalance(walletId));

			if (!walletBalanceCache.tryAtomicDebit(walletId, amount)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
			}

			try {
				return debitAction.get();
			}
			catch (RuntimeException ex) {
				walletBalanceService.rollbackCache(walletId);
				throw ex;
			}
		});
	}

}
