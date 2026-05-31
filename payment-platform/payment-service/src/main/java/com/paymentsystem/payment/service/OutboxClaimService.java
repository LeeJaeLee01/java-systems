package com.paymentsystem.payment.service;

import com.paymentsystem.common.enums.OutboxEventStatus;
import com.paymentsystem.payment.config.OutboxProperties;
import com.paymentsystem.payment.domain.OutboxEvent;
import com.paymentsystem.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {

	private final OutboxEventRepository outboxEventRepository;
	private final OutboxProperties outboxProperties;

	@Transactional
	public List<OutboxEvent> claimPendingBatch() {
		Instant now = Instant.now();
		List<OutboxEvent> events = outboxEventRepository.findPendingForDispatch(
			outboxProperties.getMaxRetries(),
			now,
			now.minusSeconds(outboxProperties.getClaimStaleSeconds()),
			outboxProperties.getBatchSize()
		);
		for (OutboxEvent event : events) {
			event.setStatus(OutboxEventStatus.PROCESSING.name());
			event.setClaimedAt(now);
		}
		return outboxEventRepository.saveAll(events);
	}

}
