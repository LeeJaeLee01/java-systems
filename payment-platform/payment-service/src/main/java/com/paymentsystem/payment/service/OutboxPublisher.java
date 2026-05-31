package com.paymentsystem.payment.service;

import com.paymentsystem.payment.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Scheduled worker that scans the local outbox and publishes events to Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

	private final OutboxClaimService outboxClaimService;
	private final OutboxDispatchService outboxDispatchService;

	@Scheduled(fixedDelayString = "${payment.outbox.poll-interval-ms:5000}")
	public void publishPendingEvents() {
		List<OutboxEvent> pendingEvents = outboxClaimService.claimPendingBatch();
		if (pendingEvents.isEmpty()) {
			return;
		}
		log.debug("Dispatching {} pending outbox event(s)", pendingEvents.size());
		for (OutboxEvent event : pendingEvents) {
			outboxDispatchService.dispatch(event.getId());
		}
	}

}
