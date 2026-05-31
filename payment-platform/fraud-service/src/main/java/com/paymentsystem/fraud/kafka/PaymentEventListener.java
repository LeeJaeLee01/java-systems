package com.paymentsystem.fraud.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentsystem.common.constant.KafkaTopics;
import com.paymentsystem.common.event.PaymentCompletedEvent;
import com.paymentsystem.fraud.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

	private final FraudDetectionService fraudDetectionService;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "fraud-service")
	public void onPaymentEvent(String payload) throws Exception {
		PaymentCompletedEvent event = objectMapper.readValue(payload, PaymentCompletedEvent.class);
		log.debug("Received payment event {} for user {}", event.eventId(), event.userId());
		fraudDetectionService.processPaymentEvent(event);
	}

}
