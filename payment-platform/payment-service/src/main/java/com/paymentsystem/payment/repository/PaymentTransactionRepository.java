package com.paymentsystem.payment.repository;

import com.paymentsystem.payment.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

	Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

}
