package com.paymentsystem.payment.service;

import com.paymentsystem.payment.domain.ProcessedStripeEvent;
import com.paymentsystem.payment.repository.ProcessedStripeEventRepository;
import com.stripe.model.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

	@Mock
	private ProcessedStripeEventRepository processedStripeEventRepository;

	@InjectMocks
	private StripeWebhookService stripeWebhookService;

	@Test
	void skipsDuplicateEvents() {
		Event event = new Event();
		event.setId("evt_duplicate");
		event.setType("payment_intent.succeeded");
		when(processedStripeEventRepository.existsByStripeEventId("evt_duplicate")).thenReturn(true);

		stripeWebhookService.handle(event);

		verify(processedStripeEventRepository, never()).save(any());
	}

	@Test
	void persistsNewEventAfterProcessing() {
		Event event = new Event();
		event.setId("evt_new");
		event.setType("customer.created");
		when(processedStripeEventRepository.existsByStripeEventId("evt_new")).thenReturn(false);

		stripeWebhookService.handle(event);

		ArgumentCaptor<ProcessedStripeEvent> captor = ArgumentCaptor.forClass(ProcessedStripeEvent.class);
		verify(processedStripeEventRepository).save(captor.capture());
		assertThat(captor.getValue().getStripeEventId()).isEqualTo("evt_new");
		assertThat(captor.getValue().getEventType()).isEqualTo("customer.created");
	}

}
