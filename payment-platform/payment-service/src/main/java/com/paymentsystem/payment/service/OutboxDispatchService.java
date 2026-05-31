package com.paymentsystem.payment.service;

import com.paymentsystem.common.constant.KafkaTopics;
import com.paymentsystem.common.enums.OutboxEventStatus;
import com.paymentsystem.payment.config.OutboxProperties;
import com.paymentsystem.payment.domain.OutboxEvent;
import com.paymentsystem.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxDispatchService {

	private final OutboxEventRepository outboxEventRepository;
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final OutboxProperties outboxProperties;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void dispatch(UUID eventId) {
		OutboxEvent event = outboxEventRepository.findById(eventId).orElse(null);
		if (event == null || !event.isProcessing()) {
			return;
		}

		try {
			SendResult<String, String> result = kafkaTemplate
				.send(KafkaTopics.PAYMENT_EVENTS, event.getAggregateId().toString(), event.getPayload())
				.get(outboxProperties.getPublishTimeoutSeconds(), TimeUnit.SECONDS);

			event.setStatus(OutboxEventStatus.PUBLISHED.name());
			event.setPublishedAt(Instant.now());
			event.setLastError(null);
			event.setClaimedAt(null);
			outboxEventRepository.save(event);

			log.info(
				"Published outbox event {} to topic {} partition {} offset {}",
				event.getId(),
				KafkaTopics.PAYMENT_EVENTS,
				result.getRecordMetadata().partition(),
				result.getRecordMetadata().offset()
			);
		}
		catch (Exception ex) {
			handlePublishFailure(event, ex);
		}
	}

	private void handlePublishFailure(OutboxEvent event, Exception ex) {
		int nextRetryCount = event.getRetryCount() + 1;
		event.setRetryCount(nextRetryCount);
		event.setLastError(truncate(ex.getMessage()));

		if (nextRetryCount >= outboxProperties.getMaxRetries()) {
			event.setStatus(OutboxEventStatus.FAILED.name());
			log.error("Outbox event {} permanently failed after {} attempts", event.getId(), nextRetryCount, ex);
		}
		else {
			long delaySeconds = outboxProperties.getInitialRetryDelaySeconds() * nextRetryCount;
			event.setStatus(OutboxEventStatus.PENDING.name());
			event.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
			event.setClaimedAt(null);
			log.warn(
				"Outbox event {} publish failed (attempt {}/{}), next retry in {}s",
				event.getId(),
				nextRetryCount,
				outboxProperties.getMaxRetries(),
				delaySeconds
			);
		}

		outboxEventRepository.save(event);
	}

	private String truncate(String message) {
		if (message == null) {
			return "Unknown error";
		}
		return message.length() <= 500 ? message : message.substring(0, 500);
	}

}
