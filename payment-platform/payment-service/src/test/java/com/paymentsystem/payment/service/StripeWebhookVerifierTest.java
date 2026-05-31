package com.paymentsystem.payment.service;

import com.paymentsystem.payment.config.StripeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripeWebhookVerifierTest {

	private StripeWebhookVerifier stripeWebhookVerifier;

	@BeforeEach
	void setUp() {
		StripeProperties properties = new StripeProperties();
		properties.setWebhookSecret("whsec_test");
		stripeWebhookVerifier = new StripeWebhookVerifier(properties);
	}

	@Test
	void rejectsMissingSignatureHeader() {
		assertThatThrownBy(() -> stripeWebhookVerifier.verifyAndParse("{}", null))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("Missing Stripe-Signature");
	}

	@Test
	void rejectsInvalidSignature() {
		assertThatThrownBy(() -> stripeWebhookVerifier.verifyAndParse("{}", "invalid"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("Invalid Stripe webhook signature");
	}

}
