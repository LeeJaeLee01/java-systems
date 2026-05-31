package com.paymentsystem.payment.reconciliation.service;

import com.paymentsystem.payment.config.StripeProperties;
import com.paymentsystem.payment.reconciliation.dto.StripePaymentSnapshot;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentListParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripePaymentIntentReportFetcher implements StripeReportFetcher {

	private static final long PAGE_SIZE = 100L;

	private final StripeProperties stripeProperties;

	@Override
	public List<StripePaymentSnapshot> fetchPaymentIntents(Instant fromInclusive, Instant toExclusive) {
		if (!stripeProperties.hasApiKey()) {
			log.warn("Stripe API key not configured; skipping partner report fetch");
			return List.of();
		}

		List<StripePaymentSnapshot> snapshots = new ArrayList<>();
		String startingAfter = null;
		boolean hasMore = true;

		while (hasMore) {
			PaymentIntentListParams.Builder paramsBuilder = PaymentIntentListParams.builder()
				.setLimit(PAGE_SIZE)
				.setCreated(PaymentIntentListParams.Created.builder()
					.setGte(fromInclusive.getEpochSecond())
					.setLt(toExclusive.getEpochSecond())
					.build());

			if (startingAfter != null) {
				paramsBuilder.setStartingAfter(startingAfter);
			}

			try {
				var page = PaymentIntent.list(paramsBuilder.build());
				for (PaymentIntent paymentIntent : page.getData()) {
					snapshots.add(toSnapshot(paymentIntent));
				}
				hasMore = Boolean.TRUE.equals(page.getHasMore()) && !page.getData().isEmpty();
				if (hasMore) {
					startingAfter = page.getData().get(page.getData().size() - 1).getId();
				}
			}
			catch (StripeException ex) {
				throw new IllegalStateException("Failed to fetch Stripe PaymentIntents: " + ex.getMessage(), ex);
			}
		}

		log.info("Fetched {} Stripe PaymentIntents for window {} — {}", snapshots.size(), fromInclusive, toExclusive);
		return snapshots;
	}

	private StripePaymentSnapshot toSnapshot(PaymentIntent paymentIntent) {
		Map<String, String> metadata = paymentIntent.getMetadata();
		return new StripePaymentSnapshot(
			paymentIntent.getId(),
			fromMinorUnit(paymentIntent.getAmount(), paymentIntent.getCurrency()),
			paymentIntent.getCurrency(),
			paymentIntent.getStatus(),
			parseUuid(metadata.get("wallet_id")),
			parseUuid(metadata.get("user_id")),
			Instant.ofEpochSecond(paymentIntent.getCreated())
		);
	}

	private BigDecimal fromMinorUnit(long amountMinor, String currency) {
		int fractionDigits = isZeroDecimalCurrency(currency) ? 0 : 2;
		BigDecimal divisor = fractionDigits == 0 ? BigDecimal.ONE : BigDecimal.valueOf(100);
		return BigDecimal.valueOf(amountMinor)
			.divide(divisor, fractionDigits, RoundingMode.UNNECESSARY);
	}

	private boolean isZeroDecimalCurrency(String currency) {
		return "jpy".equalsIgnoreCase(currency) || "krw".equalsIgnoreCase(currency);
	}

	private UUID parseUuid(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return UUID.fromString(value);
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

}
