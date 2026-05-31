package com.paymentsystem.notification.service;

import com.paymentsystem.common.constant.PaymentEventTypes;
import com.paymentsystem.common.event.PaymentCompletedEvent;
import com.paymentsystem.notification.domain.InboxEvent;
import com.paymentsystem.notification.repository.InboxEventRepository;
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
public class NotificationInboxService {

	private final InboxEventRepository inboxEventRepository;

	@Transactional
	public boolean registerIfNew(PaymentCompletedEvent event) {
		if (inboxEventRepository.existsByMessageId(event.eventId())) {
			log.info("Skip duplicate notification event {}", event.eventId());
			return false;
		}

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
			log.info("Skip concurrent duplicate notification event {}", event.eventId());
			return false;
		}
	}

}
