package com.paymentsystem.notification.sender;

import com.paymentsystem.common.event.PaymentCompletedEvent;

public interface SmsSender {

	void sendPaymentCompleted(PaymentCompletedEvent event);

}
