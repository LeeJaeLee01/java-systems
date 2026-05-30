package com.paymentsystem.payment.service;

import com.paymentsystem.common.constant.KafkaTopics;
import com.paymentsystem.payment.domain.OutboxEvent;
import com.paymentsystem.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

	private final OutboxEventRepository outboxEventRepository;
	private final KafkaTemplate<String, String> kafkaTemplate;

	@Scheduled(fixedDelayString = "5000")
	@Transactional
	public void publishPendingEvents() {
		List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
		for (OutboxEvent event : pendingEvents) {
			kafkaTemplate.send(KafkaTopics.PAYMENT_EVENTS, event.getAggregateId().toString(), event.getPayload());
			event.setStatus("PUBLISHED");
			event.setPublishedAt(Instant.now());
			log.info("Published outbox event {} to topic {}", event.getId(), KafkaTopics.PAYMENT_EVENTS);
		}
	}

}
