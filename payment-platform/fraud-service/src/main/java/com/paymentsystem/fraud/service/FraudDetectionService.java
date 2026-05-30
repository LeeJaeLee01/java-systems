package com.paymentsystem.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentsystem.common.constant.KafkaTopics;
import com.paymentsystem.common.event.PaymentCompletedEvent;
import com.paymentsystem.fraud.domain.FraudAlert;
import com.paymentsystem.fraud.domain.InboxEvent;
import com.paymentsystem.fraud.repository.FraudAlertRepository;
import com.paymentsystem.fraud.repository.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

	private final InboxEventRepository inboxEventRepository;
	private final FraudAlertRepository fraudAlertRepository;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@Value("${fraud.velocity.max-transactions-per-10-seconds}")
	private long maxTransactionsPer10Seconds;

	@KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "fraud-service")
	@Transactional
	public void consume(String payload) throws Exception {
		PaymentCompletedEvent event = objectMapper.readValue(payload, PaymentCompletedEvent.class);

		if (inboxEventRepository.existsByMessageId(event.eventId())) {
			log.info("Skip duplicate fraud event {}", event.eventId());
			return;
		}

		inboxEventRepository.save(InboxEvent.builder()
			.id(UUID.randomUUID())
			.messageId(event.eventId())
			.eventType(KafkaTopics.PAYMENT_EVENTS)
			.processedAt(Instant.now())
			.build());

		String velocityKey = "fraud:velocity:" + event.walletId();
		Long count = redisTemplate.opsForValue().increment(velocityKey);
		if (count != null && count == 1L) {
			redisTemplate.expire(velocityKey, Duration.ofSeconds(10));
		}

		if (count != null && count > maxTransactionsPer10Seconds) {
			fraudAlertRepository.save(FraudAlert.builder()
				.id(UUID.randomUUID())
				.walletId(event.walletId())
				.userId(event.userId())
				.reason("More than " + maxTransactionsPer10Seconds + " transactions within 10 seconds")
				.createdAt(Instant.now())
				.build());
			log.warn("Fraud alert created for wallet {}", event.walletId());
		}
	}

}
