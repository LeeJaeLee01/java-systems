package com.paymentsystem.payment.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(StripeProperties.class)
public class StripeConfig {

	private final StripeProperties stripeProperties;

	@PostConstruct
	void initStripe() {
		if (stripeProperties.hasApiKey()) {
			Stripe.apiKey = stripeProperties.getApiKey();
			log.info("Stripe API key configured");
		}
		else {
			log.warn("STRIPE_API_KEY is not set; PaymentIntent API will be unavailable");
		}
	}

}
