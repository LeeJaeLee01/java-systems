package com.paymentsystem.payment.service;

import com.paymentsystem.payment.config.StripeProperties;
import com.paymentsystem.payment.dto.CreatePaymentIntentRequest;
import com.paymentsystem.payment.dto.PaymentIntentResponse;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StripePaymentIntentService {

	private final StripeProperties stripeProperties;

	public PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request) {
		if (!stripeProperties.hasApiKey()) {
			throw new ResponseStatusException(
				HttpStatus.SERVICE_UNAVAILABLE,
				"Stripe API key is not configured (set STRIPE_API_KEY)"
			);
		}

		long amountInMinorUnit = toMinorUnit(request.amount(), request.currency());
		Map<String, String> metadata = new HashMap<>();
		metadata.put("wallet_id", request.walletId().toString());
		metadata.put("user_id", request.userId().toString());

		PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
			.setAmount(amountInMinorUnit)
			.setCurrency(request.currency().toLowerCase())
			.putAllMetadata(metadata)
			.setAutomaticPaymentMethods(
				PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
					.setEnabled(true)
					.build()
			)
			.build();

		try {
			PaymentIntent paymentIntent = PaymentIntent.create(params);
			return new PaymentIntentResponse(paymentIntent.getId(), paymentIntent.getClientSecret());
		}
		catch (StripeException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe PaymentIntent creation failed: " + ex.getMessage());
		}
	}

	private long toMinorUnit(BigDecimal amount, String currency) {
		int fractionDigits = currency.equalsIgnoreCase("jpy") || currency.equalsIgnoreCase("krw") ? 0 : 2;
		BigDecimal scaled = amount.setScale(fractionDigits, RoundingMode.HALF_UP);
		BigDecimal multiplier = fractionDigits == 0 ? BigDecimal.ONE : BigDecimal.valueOf(100);
		return scaled.multiply(multiplier).longValueExact();
	}

}
