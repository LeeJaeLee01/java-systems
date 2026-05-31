package com.paymentsystem.payment.service;

import com.paymentsystem.payment.domain.ProcessedStripeEvent;
import com.paymentsystem.payment.repository.ProcessedStripeEventRepository;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookService {

	private final ProcessedStripeEventRepository processedStripeEventRepository;

	@Transactional
	public void handle(Event event) {
		if (processedStripeEventRepository.existsByStripeEventId(event.getId())) {
			log.info("Ignoring duplicate Stripe event id={}", event.getId());
			return;
		}

		dispatch(event);

		processedStripeEventRepository.save(ProcessedStripeEvent.builder()
			.id(UUID.randomUUID())
			.stripeEventId(event.getId())
			.eventType(event.getType())
			.processedAt(Instant.now())
			.build());
	}

	private void dispatch(Event event) {
		switch (event.getType()) {
			case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
			case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
			default -> log.debug("Unhandled Stripe event type={} id={}", event.getType(), event.getId());
		}
	}

	private void handlePaymentIntentSucceeded(Event event) {
		findPaymentIntent(event).ifPresent(paymentIntent ->
			log.info("Stripe payment_intent.succeeded id={} amount={} currency={}",
				paymentIntent.getId(), paymentIntent.getAmount(), paymentIntent.getCurrency())
		);
	}

	private void handlePaymentIntentFailed(Event event) {
		findPaymentIntent(event).ifPresent(paymentIntent ->
			log.warn("Stripe payment_intent.payment_failed id={} lastPaymentError={}",
				paymentIntent.getId(),
				paymentIntent.getLastPaymentError() != null ? paymentIntent.getLastPaymentError().getMessage() : "unknown")
		);
	}

	private Optional<PaymentIntent> findPaymentIntent(Event event) {
		return event.getDataObjectDeserializer()
			.getObject()
			.filter(PaymentIntent.class::isInstance)
			.map(PaymentIntent.class::cast);
	}

}
