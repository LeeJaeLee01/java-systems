package com.paymentsystem.payment.exception;

import com.paymentsystem.common.enums.PaymentStateEvent;
import com.paymentsystem.common.enums.TransactionStatus;

public class IllegalPaymentStateTransitionException extends RuntimeException {

	public IllegalPaymentStateTransitionException(TransactionStatus current, PaymentStateEvent event) {
		super("Invalid payment state transition: " + current + " --[" + event + "]--> ?");
	}

}
