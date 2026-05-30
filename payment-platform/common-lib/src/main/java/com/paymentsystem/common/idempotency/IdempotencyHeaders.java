package com.paymentsystem.common.idempotency;

public final class IdempotencyHeaders {

	public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

	public static final String X_IDEMPOTENCY_KEY = "X-Idempotency-Key";

	private IdempotencyHeaders() {
	}

}
