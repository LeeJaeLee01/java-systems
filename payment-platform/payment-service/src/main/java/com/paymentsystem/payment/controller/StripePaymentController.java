package com.paymentsystem.payment.controller;

import com.paymentsystem.common.dto.ApiResponse;
import com.paymentsystem.common.idempotency.Idempotent;
import com.paymentsystem.payment.dto.CreatePaymentIntentRequest;
import com.paymentsystem.payment.dto.PaymentIntentResponse;
import com.paymentsystem.payment.service.StripePaymentIntentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripePaymentController {

	private final StripePaymentIntentService stripePaymentIntentService;

	@Idempotent
	@PostMapping("/payment-intents")
	public ApiResponse<PaymentIntentResponse> createPaymentIntent(@Valid @RequestBody CreatePaymentIntentRequest request) {
		PaymentIntentResponse response = stripePaymentIntentService.createPaymentIntent(request);
		return ApiResponse.ok("PaymentIntent created", response);
	}

}
