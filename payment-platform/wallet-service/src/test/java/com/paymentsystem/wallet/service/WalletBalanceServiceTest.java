package com.paymentsystem.wallet.service;

import com.paymentsystem.wallet.domain.Wallet;
import com.paymentsystem.wallet.domain.WalletStatus;
import com.paymentsystem.wallet.domain.WalletTransactionType;
import com.paymentsystem.wallet.dto.DepositRequest;
import com.paymentsystem.wallet.dto.WithdrawRequest;
import com.paymentsystem.wallet.repository.WalletRepository;
import com.paymentsystem.wallet.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletBalanceServiceTest {

	@Mock
	private WalletRepository walletRepository;

	@Mock
	private WalletTransactionRepository walletTransactionRepository;

	@Mock
	private RedisWalletBalanceCache walletBalanceCache;

	@InjectMocks
	private WalletBalanceService walletBalanceService;

	private UUID walletId;
	private Wallet wallet;

	@BeforeEach
	void setUp() {
		walletId = UUID.randomUUID();
		wallet = Wallet.builder()
			.id(walletId)
			.userId(UUID.randomUUID())
			.balance(new BigDecimal("100.0000"))
			.currency("USD")
			.status(WalletStatus.ACTIVE.name())
			.createdAt(Instant.now())
			.updatedAt(Instant.now())
			.build();
	}

	@Test
	void depositUsesPessimisticLockAndIncreasesBalance() {
		UUID referenceId = UUID.randomUUID();
		when(walletTransactionRepository.findByWalletIdAndReferenceIdAndTransactionType(
			walletId, referenceId, WalletTransactionType.DEPOSIT.name()
		)).thenReturn(Optional.empty());
		when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

		var response = walletBalanceService.deposit(
			walletId,
			new DepositRequest(referenceId, new BigDecimal("25.00"), "top-up")
		);

		assertThat(response.balance()).isEqualByComparingTo("125.0000");
		verify(walletRepository).findByIdForUpdate(walletId);
		verify(walletTransactionRepository).save(any());
		verify(walletBalanceCache).syncBalance(eq(walletId), eq(new BigDecimal("125.0000")));
	}

	@Test
	void withdrawFailsWhenBalanceIsInsufficient() {
		UUID referenceId = UUID.randomUUID();
		when(walletTransactionRepository.findByWalletIdAndReferenceIdAndTransactionType(
			walletId, referenceId, WalletTransactionType.WITHDRAW.name()
		)).thenReturn(Optional.empty());
		when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

		assertThatThrownBy(() -> walletBalanceService.withdraw(
			walletId,
			new WithdrawRequest(referenceId, new BigDecimal("150.00"), "cash-out")
		)).isInstanceOf(ResponseStatusException.class);

		verify(walletTransactionRepository, never()).save(any());
	}

	@Test
	void withdrawFailsWhenWalletIsFrozen() {
		wallet.setStatus(WalletStatus.FROZEN.name());
		UUID referenceId = UUID.randomUUID();
		when(walletTransactionRepository.findByWalletIdAndReferenceIdAndTransactionType(
			walletId, referenceId, WalletTransactionType.WITHDRAW.name()
		)).thenReturn(Optional.empty());
		when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));

		assertThatThrownBy(() -> walletBalanceService.withdraw(
			walletId,
			new WithdrawRequest(referenceId, new BigDecimal("10.00"), "cash-out")
		)).isInstanceOf(ResponseStatusException.class);

		verify(walletTransactionRepository, never()).save(any());
	}

}
