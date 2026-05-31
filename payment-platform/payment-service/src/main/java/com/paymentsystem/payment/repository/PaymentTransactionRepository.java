package com.paymentsystem.payment.repository;

import com.paymentsystem.payment.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

	Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

	Optional<PaymentTransaction> findByStripePaymentIntentId(String stripePaymentIntentId);

	List<PaymentTransaction> findByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
		String status,
		java.time.Instant startInclusive,
		java.time.Instant endExclusive
	);

}
