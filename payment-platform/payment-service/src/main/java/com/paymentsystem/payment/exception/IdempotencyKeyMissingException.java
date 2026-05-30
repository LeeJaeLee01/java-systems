package com.paymentsystem.payment.exception;

public class IdempotencyKeyMissingException extends RuntimeException {

	public IdempotencyKeyMissingException() {
		super("Idempotency-Key or X-Idempotency-Key header is required");
	}

}
