package com.paymentsystem.payment.service;

import com.paymentsystem.common.constant.PaymentEventTypes;
import com.paymentsystem.common.enums.OutboxEventStatus;
import com.paymentsystem.payment.config.OutboxProperties;
import com.paymentsystem.payment.domain.OutboxEvent;
import com.paymentsystem.payment.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxDispatchServiceTest {

	@Mock
	private OutboxEventRepository outboxEventRepository;

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	private OutboxDispatchService outboxDispatchService;

	@BeforeEach
	void setUp() {
		outboxDispatchService = new OutboxDispatchService(
			outboxEventRepository,
			kafkaTemplate,
			new OutboxProperties()
		);
	}

	@Test
	void marksEventPublishedAfterKafkaAck() {
		OutboxEvent event = processingEvent();
		when(outboxEventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		SendResult<String, String> sendResult = new SendResult<>(
			null,
			new RecordMetadata(new TopicPartition("payment-events", 0), 0, 0, Instant.now().toEpochMilli(), 0, 0)
		);
		when(kafkaTemplate.send(eq("payment-events"), anyString(), anyString()))
			.thenReturn(CompletableFuture.completedFuture(sendResult));

		outboxDispatchService.dispatch(event.getId());

		ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outboxEventRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED.name());
	}

	@Test
	void schedulesRetryWhenKafkaPublishFails() {
		OutboxEvent event = processingEvent();
		when(outboxEventRepository.findById(event.getId())).thenReturn(Optional.of(event));
		when(kafkaTemplate.send(eq("payment-events"), anyString(), anyString()))
			.thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

		outboxDispatchService.dispatch(event.getId());

		ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outboxEventRepository).save(captor.capture());
		OutboxEvent saved = captor.getValue();
		assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING.name());
		assertThat(saved.getRetryCount()).isEqualTo(1);
	}

	private OutboxEvent processingEvent() {
		return OutboxEvent.builder()
			.id(UUID.randomUUID())
			.aggregateType("PAYMENT")
			.aggregateId(UUID.randomUUID())
			.eventType(PaymentEventTypes.PAYMENT_COMPLETED)
			.payload("{\"eventId\":\"" + UUID.randomUUID() + "\"}")
			.status(OutboxEventStatus.PROCESSING.name())
			.createdAt(Instant.now())
			.claimedAt(Instant.now())
			.retryCount(0)
			.build();
	}

}
