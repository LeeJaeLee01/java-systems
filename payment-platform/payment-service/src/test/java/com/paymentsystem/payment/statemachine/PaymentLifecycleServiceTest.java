package com.paymentsystem.payment.statemachine;

import com.paymentsystem.common.enums.PaymentStateEvent;
import com.paymentsystem.common.enums.TransactionStatus;
import com.paymentsystem.payment.domain.PaymentTransaction;
import com.paymentsystem.payment.exception.IllegalPaymentStateTransitionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(classes = { PaymentStateMachineConfig.class, PaymentLifecycleService.class })
class PaymentLifecycleServiceTest {

	@Autowired
	private PaymentLifecycleService paymentLifecycleService;

	@Test
	void allowsPendingToProcessingToSuccess() {
		PaymentTransaction payment = samplePayment(TransactionStatus.PENDING.name());

		paymentLifecycleService.transition(payment, PaymentStateEvent.START_PROCESSING);
		assertThat(payment.getStatus()).isEqualTo(TransactionStatus.PROCESSING.name());

		paymentLifecycleService.transition(payment, PaymentStateEvent.COMPLETE);
		assertThat(payment.getStatus()).isEqualTo(TransactionStatus.SUCCESS.name());
	}

	@Test
	void allowsProcessingToFailed() {
		PaymentTransaction payment = samplePayment(TransactionStatus.PROCESSING.name());

		paymentLifecycleService.transition(payment, PaymentStateEvent.FAIL);
		assertThat(payment.getStatus()).isEqualTo(TransactionStatus.FAILED.name());
	}

	@Test
	void allowsProcessingToFraudRejected() {
		PaymentTransaction payment = samplePayment(TransactionStatus.PROCESSING.name());

		paymentLifecycleService.transition(payment, PaymentStateEvent.FRAUD_DETECT);
		assertThat(payment.getStatus()).isEqualTo(TransactionStatus.FRAUD_REJECTED.name());
	}

	@Test
	void rejectsInvalidTransitionFromSuccess() {
		PaymentTransaction payment = samplePayment(TransactionStatus.SUCCESS.name());

		assertThatThrownBy(() -> paymentLifecycleService.transition(payment, PaymentStateEvent.FAIL))
			.isInstanceOf(IllegalPaymentStateTransitionException.class);
	}

	@Test
	void rejectsCompleteFromPending() {
		PaymentTransaction payment = samplePayment(TransactionStatus.PENDING.name());

		assertThatThrownBy(() -> paymentLifecycleService.transition(payment, PaymentStateEvent.COMPLETE))
			.isInstanceOf(IllegalPaymentStateTransitionException.class);
	}

	private PaymentTransaction samplePayment(String status) {
		Instant now = Instant.now();
		return PaymentTransaction.builder()
			.id(UUID.randomUUID())
			.walletId(UUID.randomUUID())
			.userId(UUID.randomUUID())
			.idempotencyKey("key-" + UUID.randomUUID())
			.amount(new BigDecimal("1.00"))
			.status(status)
			.createdAt(now)
			.updatedAt(now)
			.build();
	}

}
