package com.paymentsystem.notification.service;

import com.paymentsystem.common.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentNotificationService {

	private final NotificationInboxService notificationInboxService;
	private final NotificationDispatchService notificationDispatchService;

	public void handlePaymentCompleted(PaymentCompletedEvent event) {
		if (!notificationInboxService.registerIfNew(event)) {
			return;
		}
		notificationDispatchService.dispatchAsync(event);
	}

}
