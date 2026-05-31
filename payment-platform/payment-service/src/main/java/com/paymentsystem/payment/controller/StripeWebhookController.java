package com.paymentsystem.payment.controller;

import com.paymentsystem.payment.service.StripeWebhookService;
import com.paymentsystem.payment.service.StripeWebhookVerifier;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

	private final StripeWebhookVerifier stripeWebhookVerifier;
	private final StripeWebhookService stripeWebhookService;

	@PostMapping("/webhook")
	public ResponseEntity<String> handleWebhook(
		@RequestBody String payload,
		@RequestHeader(value = "Stripe-Signature", required = false) String signatureHeader
	) {
		Event event = stripeWebhookVerifier.verifyAndParse(payload, signatureHeader);
		stripeWebhookService.handle(event);
		return ResponseEntity.ok("ok");
	}

}
