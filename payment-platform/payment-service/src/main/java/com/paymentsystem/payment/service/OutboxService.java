package com.paymentsystem.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentsystem.common.constant.PaymentEventTypes;
import com.paymentsystem.common.enums.OutboxEventStatus;
import com.paymentsystem.common.event.PaymentCompletedEvent;
import com.paymentsystem.payment.domain.OutboxEvent;
import com.paymentsystem.payment.domain.PaymentTransaction;
import com.paymentsystem.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Writes domain events to the local outbox table within the caller's transaction.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

	private static final String AGGREGATE_TYPE = "PAYMENT";

	private final OutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper;

	public void enqueuePaymentCompleted(PaymentTransaction payment) {
		PaymentCompletedEvent event = new PaymentCompletedEvent(
			UUID.randomUUID(),
			payment.getId(),
			payment.getWalletId(),
			payment.getUserId(),
			payment.getAmount(),
			payment.getStatus(),
			Instant.now()
		);

		try {
			outboxEventRepository.save(OutboxEvent.builder()
				.id(UUID.randomUUID())
				.aggregateType(AGGREGATE_TYPE)
				.aggregateId(payment.getId())
				.eventType(PaymentEventTypes.PAYMENT_COMPLETED)
				.payload(objectMapper.writeValueAsString(event))
				.status(OutboxEventStatus.PENDING.name())
				.createdAt(Instant.now())
				.retryCount(0)
				.build());
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Failed to serialize outbox payload", ex);
		}
	}

}
