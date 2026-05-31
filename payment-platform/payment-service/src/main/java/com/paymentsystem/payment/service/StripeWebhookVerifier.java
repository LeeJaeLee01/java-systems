package com.paymentsystem.payment.service;

import com.paymentsystem.payment.config.StripeProperties;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class StripeWebhookVerifier {

	private final StripeProperties stripeProperties;

	public Event verifyAndParse(String payload, String signatureHeader) {
		if (!stripeProperties.hasWebhookSecret()) {
			throw new ResponseStatusException(
				HttpStatus.SERVICE_UNAVAILABLE,
				"Stripe webhook secret is not configured (set STRIPE_WEBHOOK_SECRET)"
			);
		}
		if (signatureHeader == null || signatureHeader.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Stripe-Signature header");
		}
		try {
			return Webhook.constructEvent(
				payload,
				signatureHeader,
				stripeProperties.getWebhookSecret(),
				stripeProperties.getWebhookToleranceSeconds()
			);
		}
		catch (SignatureVerificationException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook signature");
		}
	}

}
