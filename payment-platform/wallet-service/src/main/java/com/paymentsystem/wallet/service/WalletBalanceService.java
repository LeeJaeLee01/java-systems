package com.paymentsystem.wallet.service;

import com.paymentsystem.wallet.domain.Wallet;
import com.paymentsystem.wallet.domain.WalletStatus;
import com.paymentsystem.wallet.domain.WalletTransaction;
import com.paymentsystem.wallet.domain.WalletTransactionType;
import com.paymentsystem.wallet.dto.CreateWalletRequest;
import com.paymentsystem.wallet.dto.DepositRequest;
import com.paymentsystem.wallet.dto.WalletOperationRequest;
import com.paymentsystem.wallet.dto.WalletResponse;
import com.paymentsystem.wallet.dto.WalletTransactionResponse;
import com.paymentsystem.wallet.dto.WithdrawRequest;
import com.paymentsystem.wallet.repository.WalletRepository;
import com.paymentsystem.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletBalanceService {

	private final WalletRepository walletRepository;
	private final WalletTransactionRepository walletTransactionRepository;
	private final RedisWalletBalanceCache walletBalanceCache;

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
			.status(WalletStatus.ACTIVE.name())
			.createdAt(now)
			.updatedAt(now)
			.build();

		Wallet saved = walletRepository.save(wallet);
		walletBalanceCache.syncBalance(saved.getId(), saved.getBalance());
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public WalletResponse getWalletByUserId(UUID userId) {
		return walletRepository.findByUserId(userId)
			.map(this::toResponse)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
	}

	@Transactional(readOnly = true)
	public WalletResponse getWalletById(UUID walletId) {
		return walletRepository.findById(walletId)
			.map(this::toResponse)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
	}

	@Transactional(readOnly = true)
	public List<WalletTransactionResponse> getTransactions(UUID walletId) {
		ensureWalletExists(walletId);
		return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId).stream()
			.map(this::toTransactionResponse)
			.toList();
	}

	@Transactional
	public WalletResponse deposit(UUID walletId, DepositRequest request) {
		return applyCredit(
			walletId,
			request.referenceId(),
			request.amount(),
			WalletTransactionType.DEPOSIT,
			"DEPOSIT"
		);
	}

	@Transactional
	public WalletResponse withdraw(UUID walletId, WithdrawRequest request) {
		return applyDebit(
			walletId,
			request.referenceId(),
			request.amount(),
			WalletTransactionType.WITHDRAW,
			"WITHDRAW"
		);
	}

	@Transactional
	public WalletResponse credit(UUID walletId, WalletOperationRequest request) {
		return applyCredit(
			walletId,
			request.referenceId(),
			request.amount(),
			WalletTransactionType.CREDIT,
			"PAYMENT"
		);
	}

	@Transactional
	public WalletResponse debit(UUID walletId, WalletOperationRequest request) {
		return applyDebit(
			walletId,
			request.referenceId(),
			request.amount(),
			WalletTransactionType.DEBIT,
			"PAYMENT"
		);
	}

	BigDecimal loadBalance(UUID walletId) {
		return walletRepository.findById(walletId)
			.map(Wallet::getBalance)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
	}

	void rollbackCache(UUID walletId) {
		walletBalanceCache.syncBalance(walletId, loadBalance(walletId));
	}

	private WalletResponse applyCredit(
		UUID walletId,
		UUID referenceId,
		BigDecimal amount,
		WalletTransactionType transactionType,
		String referenceType
	) {
		WalletTransaction existing = findExistingTransaction(walletId, referenceId, transactionType);
		if (existing != null) {
			return getWalletById(walletId);
		}

		Wallet wallet = lockWallet(walletId);
		ensureWalletActive(wallet);

		BigDecimal balanceBefore = wallet.getBalance();
		BigDecimal balanceAfter = balanceBefore.add(amount);
		wallet.setBalance(balanceAfter);
		wallet.setUpdatedAt(Instant.now());

		saveLedger(wallet, referenceId, amount, balanceBefore, balanceAfter, transactionType, referenceType);
		walletBalanceCache.syncBalance(walletId, balanceAfter);
		return toResponse(wallet);
	}

	private WalletResponse applyDebit(
		UUID walletId,
		UUID referenceId,
		BigDecimal amount,
		WalletTransactionType transactionType,
		String referenceType
	) {
		WalletTransaction existing = findExistingTransaction(walletId, referenceId, transactionType);
		if (existing != null) {
			return getWalletById(walletId);
		}

		Wallet wallet = lockWallet(walletId);
		ensureWalletActive(wallet);

		BigDecimal balanceBefore = wallet.getBalance();
		if (balanceBefore.compareTo(amount) < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
		}

		BigDecimal balanceAfter = balanceBefore.subtract(amount);
		wallet.setBalance(balanceAfter);
		wallet.setUpdatedAt(Instant.now());

		saveLedger(wallet, referenceId, amount, balanceBefore, balanceAfter, transactionType, referenceType);
		walletBalanceCache.syncBalance(walletId, balanceAfter);
		return toResponse(wallet);
	}

	private Wallet lockWallet(UUID walletId) {
		return walletRepository.findByIdForUpdate(walletId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
	}

	private void ensureWalletExists(UUID walletId) {
		if (!walletRepository.existsById(walletId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found");
		}
	}

	private void ensureWalletActive(Wallet wallet) {
		if (!WalletStatus.ACTIVE.name().equals(wallet.getStatus())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wallet is not active");
		}
	}

	private WalletTransaction findExistingTransaction(
		UUID walletId,
		UUID referenceId,
		WalletTransactionType transactionType
	) {
		return walletTransactionRepository
			.findByWalletIdAndReferenceIdAndTransactionType(
				walletId,
				referenceId,
				transactionType.name()
			)
			.orElse(null);
	}

	private void saveLedger(
		Wallet wallet,
		UUID referenceId,
		BigDecimal amount,
		BigDecimal balanceBefore,
		BigDecimal balanceAfter,
		WalletTransactionType transactionType,
		String referenceType
	) {
		walletTransactionRepository.save(WalletTransaction.builder()
			.id(UUID.randomUUID())
			.walletId(wallet.getId())
			.amount(amount)
			.balanceBefore(balanceBefore)
			.balanceAfter(balanceAfter)
			.transactionType(transactionType.name())
			.referenceType(referenceType)
			.referenceId(referenceId)
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

	private WalletTransactionResponse toTransactionResponse(WalletTransaction transaction) {
		return new WalletTransactionResponse(
			transaction.getId(),
			transaction.getWalletId(),
			transaction.getAmount(),
			transaction.getBalanceBefore(),
			transaction.getBalanceAfter(),
			transaction.getTransactionType(),
			transaction.getReferenceType(),
			transaction.getReferenceId(),
			transaction.getCreatedAt()
		);
	}

}
