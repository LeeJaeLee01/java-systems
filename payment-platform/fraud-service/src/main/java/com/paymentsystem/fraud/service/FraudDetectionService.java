package com.paymentsystem.fraud.service;

import com.paymentsystem.common.constant.PaymentEventTypes;
import com.paymentsystem.common.event.PaymentCompletedEvent;
import com.paymentsystem.fraud.domain.FraudAlert;
import com.paymentsystem.fraud.domain.InboxEvent;
import com.paymentsystem.fraud.repository.FraudAlertRepository;
import com.paymentsystem.fraud.repository.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

	public static final String ALERT_TYPE_HIGH_FREQUENCY = "HIGH_FREQUENCY";

	private final InboxEventRepository inboxEventRepository;
	private final FraudAlertRepository fraudAlertRepository;
	private final SlidingWindowVelocityService slidingWindowVelocityService;

	@Transactional
	public void processPaymentEvent(PaymentCompletedEvent event) {
		if (inboxEventRepository.existsByMessageId(event.eventId())) {
			log.info("Skip duplicate fraud event {}", event.eventId());
			return;
		}

		if (!registerInbox(event)) {
			return;
		}

		VelocityCheckResult velocity = slidingWindowVelocityService.checkUserVelocity(
			event.userId(),
			event.eventId(),
			event.occurredAt() != null ? event.occurredAt() : Instant.now()
		);

		if (velocity.exceeded()) {
			createHighFrequencyAlert(event, velocity);
		}
	}

	private boolean registerInbox(PaymentCompletedEvent event) {
		try {
			inboxEventRepository.save(InboxEvent.builder()
				.id(UUID.randomUUID())
				.messageId(event.eventId())
				.eventType(PaymentEventTypes.PAYMENT_COMPLETED)
				.processedAt(Instant.now())
				.build());
			return true;
		}
		catch (DataIntegrityViolationException ex) {
			log.info("Skip concurrent duplicate fraud event {}", event.eventId());
			return false;
		}
	}

	private void createHighFrequencyAlert(PaymentCompletedEvent event, VelocityCheckResult velocity) {
		String reason = "User exceeded velocity limit: %d transactions in %d seconds (max %d)".formatted(
			velocity.transactionCount(),
			velocity.windowSeconds(),
			velocity.maxTransactions()
		);

		fraudAlertRepository.save(FraudAlert.builder()
			.id(UUID.randomUUID())
			.walletId(event.walletId())
			.userId(event.userId())
			.paymentId(event.paymentId())
			.alertType(ALERT_TYPE_HIGH_FREQUENCY)
			.transactionCount((int) velocity.transactionCount())
			.reason(reason)
			.createdAt(Instant.now())
			.build());

		log.warn(
			"Fraud alert [{}] for user {} wallet {} payment {} — {}",
			ALERT_TYPE_HIGH_FREQUENCY,
			event.userId(),
			event.walletId(),
			event.paymentId(),
			reason
		);
	}

}
