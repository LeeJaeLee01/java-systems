package com.paymentsystem.notification.sender;

import com.paymentsystem.common.event.PaymentCompletedEvent;

public interface EmailSender {

	void sendPaymentCompleted(PaymentCompletedEvent event);

}
