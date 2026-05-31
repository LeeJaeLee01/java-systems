package com.paymentsystem.notification.service;

import com.paymentsystem.common.event.PaymentCompletedEvent;
import com.paymentsystem.notification.domain.InboxEvent;
import com.paymentsystem.notification.repository.InboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class NotificationInboxServiceTest {

	@Mock
	private InboxEventRepository inboxEventRepository;

	@InjectMocks
	private NotificationInboxService notificationInboxService;

	@Test
	void registerIfNew_returnsFalseWhenDuplicate() {
		PaymentCompletedEvent event = sampleEvent();
		when(inboxEventRepository.existsByMessageId(event.eventId())).thenReturn(true);

		assertThat(notificationInboxService.registerIfNew(event)).isFalse();
		verify(inboxEventRepository, never()).save(any());
	}

	@Test
	void registerIfNew_persistsAndReturnsTrueForNewEvent() {
		PaymentCompletedEvent event = sampleEvent();
		when(inboxEventRepository.existsByMessageId(event.eventId())).thenReturn(false);

		assertThat(notificationInboxService.registerIfNew(event)).isTrue();
		verify(inboxEventRepository).save(any(InboxEvent.class));
	}

	private PaymentCompletedEvent sampleEvent() {
		return new PaymentCompletedEvent(
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			new BigDecimal("25.00"),
			"SUCCESS",
			Instant.now()
		);
	}

}
