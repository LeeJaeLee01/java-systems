package com.paymentsystem.notification.service;

import com.paymentsystem.common.event.PaymentCompletedEvent;
import com.paymentsystem.notification.sender.EmailSender;
import com.paymentsystem.notification.sender.SmsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

	@Mock
	private EmailSender emailSender;

	@Mock
	private SmsSender smsSender;

	private NotificationDispatchService dispatchService;

	@BeforeEach
	void setUp() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(10);
		executor.initialize();
		dispatchService = new NotificationDispatchService(executor, emailSender, smsSender);
	}

	@Test
	void dispatchAsync_sendsEmailAndSmsOnWorkerThread() {
		PaymentCompletedEvent event = sampleEvent();

		dispatchService.dispatchAsync(event);

		verify(emailSender, timeout(2000)).sendPaymentCompleted(event);
		verify(smsSender, timeout(2000)).sendPaymentCompleted(event);
	}

	private PaymentCompletedEvent sampleEvent() {
		return new PaymentCompletedEvent(
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			new BigDecimal("10.00"),
			"SUCCESS",
			Instant.now()
		);
	}

}
