package com.paymentsystem.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentsystem.common.constant.PaymentEventTypes;
import com.paymentsystem.common.enums.OutboxEventStatus;
import com.paymentsystem.common.enums.TransactionStatus;
import com.paymentsystem.common.event.PaymentCompletedEvent;
import com.paymentsystem.payment.domain.OutboxEvent;
import com.paymentsystem.payment.domain.PaymentTransaction;
import com.paymentsystem.payment.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

	@Mock
	private OutboxEventRepository outboxEventRepository;

	private OutboxService outboxService;

	@BeforeEach
	void setUp() {
		outboxService = new OutboxService(outboxEventRepository, new ObjectMapper().findAndRegisterModules());
	}

	@Test
	void enqueuePaymentCompletedPersistsPendingEvent() throws Exception {
		PaymentTransaction payment = PaymentTransaction.builder()
			.id(UUID.randomUUID())
			.walletId(UUID.randomUUID())
			.userId(UUID.randomUUID())
			.idempotencyKey("key-1")
			.amount(new BigDecimal("10.00"))
			.status(TransactionStatus.SUCCESS.name())
			.createdAt(Instant.now())
			.updatedAt(Instant.now())
			.build();

		outboxService.enqueuePaymentCompleted(payment);

		ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outboxEventRepository).save(captor.capture());

		OutboxEvent saved = captor.getValue();
		assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING.name());
		assertThat(saved.getEventType()).isEqualTo(PaymentEventTypes.PAYMENT_COMPLETED);

		PaymentCompletedEvent payload = new ObjectMapper().findAndRegisterModules()
			.readValue(saved.getPayload(), PaymentCompletedEvent.class);
		assertThat(payload.paymentId()).isEqualTo(payment.getId());
	}

}
