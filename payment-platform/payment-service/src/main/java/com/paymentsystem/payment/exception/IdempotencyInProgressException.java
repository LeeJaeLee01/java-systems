package com.paymentsystem.payment.exception;

public class IdempotencyInProgressException extends RuntimeException {

	public IdempotencyInProgressException() {
		super("Duplicate request is already being processed");
	}

}
