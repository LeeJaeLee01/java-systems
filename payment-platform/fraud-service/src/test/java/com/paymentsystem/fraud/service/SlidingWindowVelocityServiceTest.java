package com.paymentsystem.fraud.service;

import com.paymentsystem.fraud.config.FraudVelocityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlidingWindowVelocityServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ZSetOperations<String, String> zSetOperations;

	private SlidingWindowVelocityService slidingWindowVelocityService;

	@BeforeEach
	void setUp() {
		FraudVelocityProperties properties = new FraudVelocityProperties();
		properties.setWindowSeconds(10);
		properties.setMaxTransactions(5);
		slidingWindowVelocityService = new SlidingWindowVelocityService(redisTemplate, properties);
		when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
	}

	@Test
	void recordsEventInSortedSetAndPrunesExpiredEntries() {
		UUID userId = UUID.randomUUID();
		UUID eventId = UUID.randomUUID();
		Instant occurredAt = Instant.parse("2026-05-30T12:00:05Z");

		when(zSetOperations.zCard("fraud:velocity:user:" + userId)).thenReturn(3L);

		VelocityCheckResult result = slidingWindowVelocityService.checkUserVelocity(userId, eventId, occurredAt);

		verify(zSetOperations).add(
			eq("fraud:velocity:user:" + userId),
			eq(eventId.toString()),
			eq((double) occurredAt.toEpochMilli())
		);

		ArgumentCaptor<Double> minScoreCaptor = ArgumentCaptor.forClass(Double.class);
		verify(zSetOperations).removeRangeByScore(
			eq("fraud:velocity:user:" + userId),
			eq(0.0),
			minScoreCaptor.capture()
		);
		assertThat(minScoreCaptor.getValue())
			.isEqualTo(occurredAt.toEpochMilli() - 10_000L);

		verify(redisTemplate).expire(eq("fraud:velocity:user:" + userId), eq(Duration.ofSeconds(15)));
		assertThat(result.exceeded()).isFalse();
		assertThat(result.transactionCount()).isEqualTo(3);
	}

	@Test
	void flagsExceededVelocityThreshold() {
		UUID userId = UUID.randomUUID();
		when(zSetOperations.zCard(anyString())).thenReturn(6L);

		VelocityCheckResult result = slidingWindowVelocityService.checkUserVelocity(
			userId,
			UUID.randomUUID(),
			Instant.now()
		);

		assertThat(result.exceeded()).isTrue();
		assertThat(result.transactionCount()).isEqualTo(6);
	}

}
