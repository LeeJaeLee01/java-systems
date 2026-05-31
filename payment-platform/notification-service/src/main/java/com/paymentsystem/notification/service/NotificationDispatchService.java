package com.paymentsystem.notification.service;

import com.paymentsystem.common.event.PaymentCompletedEvent;
import com.paymentsystem.notification.sender.EmailSender;
import com.paymentsystem.notification.sender.SmsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationDispatchService {

	private final ThreadPoolTaskExecutor notificationTaskExecutor;
	private final EmailSender emailSender;
	private final SmsSender smsSender;

	public NotificationDispatchService(
		@Qualifier("notificationTaskExecutor") ThreadPoolTaskExecutor notificationTaskExecutor,
		EmailSender emailSender,
		SmsSender smsSender
	) {
		this.notificationTaskExecutor = notificationTaskExecutor;
		this.emailSender = emailSender;
		this.smsSender = smsSender;
	}

	public void dispatchAsync(PaymentCompletedEvent event) {
		notificationTaskExecutor.execute(() -> sendNotifications(event));
	}

	private void sendNotifications(PaymentCompletedEvent event) {
		try {
			emailSender.sendPaymentCompleted(event);
			smsSender.sendPaymentCompleted(event);
			log.info(
				"Notifications dispatched for payment {} wallet {} amount {}",
				event.paymentId(),
				event.walletId(),
				event.amount()
			);
		}
		catch (Exception ex) {
			log.error("Failed to send notifications for payment {}", event.paymentId(), ex);
		}
	}

}
