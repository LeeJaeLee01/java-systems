package com.paymentsystem.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentsystem.common.dto.ApiResponse;
import com.paymentsystem.payment.config.IdempotencyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private IdempotencyService idempotencyService;

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		IdempotencyProperties properties = new IdempotencyProperties();
		idempotencyService = new IdempotencyService(redisTemplate, new ObjectMapper(), properties);
	}

	@Test
	void findCachedResponseReturnsEmptyWhenMissing() {
		when(valueOperations.get("idempotency:response:key-1")).thenReturn(null);

		Optional<Object> cached = idempotencyService.findCachedResponse("key-1", ApiResponse.class);

		assertThat(cached).isEmpty();
	}

	@Test
	void tryAcquireProcessingLockUsesSetIfAbsent() {
		when(valueOperations.setIfAbsent(eq("idempotency:processing:key-2"), eq("1"), any(Duration.class)))
			.thenReturn(true);

		assertThat(idempotencyService.tryAcquireProcessingLock("key-2")).isTrue();
	}

	@Test
	void saveResponseStoresPayloadAndClearsProcessingLock() throws Exception {
		ApiResponse<String> response = ApiResponse.ok("done");

		idempotencyService.saveResponse("key-3", response);

		ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
		verify(valueOperations).set(
			eq("idempotency:response:key-3"),
			payloadCaptor.capture(),
			eq(Duration.ofHours(24))
		);
		assertThat(payloadCaptor.getValue()).contains("\"success\":true");
		verify(redisTemplate).delete("idempotency:processing:key-3");
	}

}
