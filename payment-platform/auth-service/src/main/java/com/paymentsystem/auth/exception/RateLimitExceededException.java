package com.paymentsystem.auth.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

	private final boolean blocked;

	private RateLimitExceededException(String message, boolean blocked) {
		super(message);
		this.blocked = blocked;
	}

	public static RateLimitExceededException blocked() {
		return new RateLimitExceededException("IP is temporarily blocked due to repeated auth abuse", true);
	}

	public static RateLimitExceededException exceeded() {
		return new RateLimitExceededException("Too many auth requests from this IP", false);
	}

}
