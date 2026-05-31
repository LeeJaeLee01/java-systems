package com.paymentsystem.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentsystem.common.constant.KafkaTopics;
import com.paymentsystem.common.constant.PaymentEventTypes;
import com.paymentsystem.common.event.PaymentCompletedEvent;
import com.paymentsystem.notification.domain.InboxEvent;
import com.paymentsystem.notification.repository.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventConsumer {

	private final InboxEventRepository inboxEventRepository;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "notification-service")
	@Transactional
	public void consume(String payload) throws Exception {
		PaymentCompletedEvent event = objectMapper.readValue(payload, PaymentCompletedEvent.class);

		if (inboxEventRepository.existsByMessageId(event.eventId())) {
			log.info("Skip duplicate notification event {}", event.eventId());
			return;
		}

		try {
			inboxEventRepository.save(InboxEvent.builder()
				.id(UUID.randomUUID())
				.messageId(event.eventId())
				.eventType(PaymentEventTypes.PAYMENT_COMPLETED)
				.processedAt(Instant.now())
				.build());
		}
		catch (DataIntegrityViolationException ex) {
			log.info("Skip concurrent duplicate notification event {}", event.eventId());
			return;
		}
		log.info(
			"Notification sent for payment {} wallet {} amount {}",
			event.paymentId(),
			event.walletId(),
			event.amount()
		);
	}

}
