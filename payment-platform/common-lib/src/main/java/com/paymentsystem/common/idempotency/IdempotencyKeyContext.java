package com.paymentsystem.common.idempotency;

public final class IdempotencyKeyContext {

	private static final ThreadLocal<String> CURRENT_KEY = new ThreadLocal<>();

	private IdempotencyKeyContext() {
	}

	public static void set(String key) {
		CURRENT_KEY.set(key);
	}

	public static String get() {
		return CURRENT_KEY.get();
	}

	public static String require() {
		String key = CURRENT_KEY.get();
		if (key == null || key.isBlank()) {
			throw new IllegalStateException("Idempotency key is not available in the current request context");
		}
		return key;
	}

	public static void clear() {
		CURRENT_KEY.remove();
	}

}
