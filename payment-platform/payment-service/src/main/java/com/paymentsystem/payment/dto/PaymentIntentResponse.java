package com.paymentsystem.payment.dto;

public record PaymentIntentResponse(
	String paymentIntentId,
	String clientSecret
) {
}
