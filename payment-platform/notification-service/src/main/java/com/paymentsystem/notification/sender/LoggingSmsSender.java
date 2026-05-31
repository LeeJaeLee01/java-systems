package com.paymentsystem.notification.sender;

import com.paymentsystem.common.event.PaymentCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.sms.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingSmsSender implements SmsSender {

	@Override
	public void sendPaymentCompleted(PaymentCompletedEvent event) {
		log.info(
			"[SMS] Payment completed — paymentId={} userId={} amount={}",
			event.paymentId(),
			event.userId(),
			event.amount()
		);
	}

}
