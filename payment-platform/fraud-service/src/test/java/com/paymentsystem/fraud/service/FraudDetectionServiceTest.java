package com.paymentsystem.fraud.service;

import com.paymentsystem.common.event.PaymentCompletedEvent;
import com.paymentsystem.fraud.domain.FraudAlert;
import com.paymentsystem.fraud.repository.FraudAlertRepository;
import com.paymentsystem.fraud.repository.InboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

	@Mock
	private InboxEventRepository inboxEventRepository;

	@Mock
	private FraudAlertRepository fraudAlertRepository;

	@Mock
	private SlidingWindowVelocityService slidingWindowVelocityService;

	@InjectMocks
	private FraudDetectionService fraudDetectionService;

	@Test
	void createsAlertWhenVelocityExceeded() {
		PaymentCompletedEvent event = sampleEvent();
		when(inboxEventRepository.existsByMessageId(event.eventId())).thenReturn(false);
		when(slidingWindowVelocityService.checkUserVelocity(event.userId(), event.eventId(), event.occurredAt()))
			.thenReturn(new VelocityCheckResult(event.userId(), 6, 10, 5, true));

		fraudDetectionService.processPaymentEvent(event);

		ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
		verify(fraudAlertRepository).save(captor.capture());
		FraudAlert alert = captor.getValue();
		assertThat(alert.getAlertType()).isEqualTo(FraudDetectionService.ALERT_TYPE_HIGH_FREQUENCY);
		assertThat(alert.getTransactionCount()).isEqualTo(6);
		assertThat(alert.getUserId()).isEqualTo(event.userId());
	}

	@Test
	void skipsAlertWhenVelocityWithinLimit() {
		PaymentCompletedEvent event = sampleEvent();
		when(inboxEventRepository.existsByMessageId(event.eventId())).thenReturn(false);
		when(slidingWindowVelocityService.checkUserVelocity(event.userId(), event.eventId(), event.occurredAt()))
			.thenReturn(new VelocityCheckResult(event.userId(), 2, 10, 5, false));

		fraudDetectionService.processPaymentEvent(event);

		verify(fraudAlertRepository, never()).save(any());
	}

	private PaymentCompletedEvent sampleEvent() {
		return new PaymentCompletedEvent(
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			new BigDecimal("10.00"),
			"SUCCESS",
			Instant.parse("2026-05-30T12:00:00Z")
		);
	}

}
