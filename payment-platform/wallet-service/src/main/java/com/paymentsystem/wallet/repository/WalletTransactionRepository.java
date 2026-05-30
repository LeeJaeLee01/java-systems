package com.paymentsystem.wallet.repository;

import com.paymentsystem.wallet.domain.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

	boolean existsByWalletIdAndReferenceIdAndTransactionType(
		UUID walletId,
		UUID referenceId,
		String transactionType
	);

	Optional<WalletTransaction> findByWalletIdAndReferenceIdAndTransactionType(
		UUID walletId,
		UUID referenceId,
		String transactionType
	);

	List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId);

}
