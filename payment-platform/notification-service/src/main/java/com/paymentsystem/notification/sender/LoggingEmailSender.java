package com.paymentsystem.notification.sender;

import com.paymentsystem.common.event.PaymentCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.ses.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

	@Override
	public void sendPaymentCompleted(PaymentCompletedEvent event) {
		log.info(
			"[EMAIL] Payment completed — paymentId={} userId={} amount={} status={}",
			event.paymentId(),
			event.userId(),
			event.amount(),
			event.status()
		);
	}

}
