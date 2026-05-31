package com.paymentsystem.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentsystem.common.constant.KafkaTopics;
import com.paymentsystem.common.event.PaymentCompletedEvent;
import com.paymentsystem.notification.service.PaymentNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedEventListener {

	private final ObjectMapper objectMapper;
	private final PaymentNotificationService paymentNotificationService;

	/**
	 * Consumes {@code payment-completed} payloads published on {@link KafkaTopics#PAYMENT_EVENTS}.
	 */
	@KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "notification-service")
	public void onPaymentCompleted(String payload) throws Exception {
		PaymentCompletedEvent event = objectMapper.readValue(payload, PaymentCompletedEvent.class);
		log.debug("Received payment-completed event {} paymentId={}", event.eventId(), event.paymentId());
		paymentNotificationService.handlePaymentCompleted(event);
	}

}
