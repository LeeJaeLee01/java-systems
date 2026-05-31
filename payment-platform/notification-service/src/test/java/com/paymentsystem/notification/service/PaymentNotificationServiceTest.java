package com.paymentsystem.notification.service;

import com.paymentsystem.common.event.PaymentCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationServiceTest {

	@Mock
	private NotificationInboxService notificationInboxService;

	@Mock
	private NotificationDispatchService notificationDispatchService;

	@InjectMocks
	private PaymentNotificationService paymentNotificationService;

	@Test
	void handlePaymentCompleted_skipsDispatchWhenDuplicate() {
		PaymentCompletedEvent event = sampleEvent();
		when(notificationInboxService.registerIfNew(event)).thenReturn(false);

		paymentNotificationService.handlePaymentCompleted(event);

		verify(notificationDispatchService, never()).dispatchAsync(event);
	}

	@Test
	void handlePaymentCompleted_dispatchesWhenNew() {
		PaymentCompletedEvent event = sampleEvent();
		when(notificationInboxService.registerIfNew(event)).thenReturn(true);

		paymentNotificationService.handlePaymentCompleted(event);

		verify(notificationDispatchService).dispatchAsync(event);
	}

	private PaymentCompletedEvent sampleEvent() {
		return new PaymentCompletedEvent(
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			new BigDecimal("50.00"),
			"SUCCESS",
			Instant.now()
		);
	}

}
