package com.paymentsystem.auth.service;

import com.paymentsystem.auth.config.AuthRateLimitProperties;
import com.paymentsystem.auth.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRateLimiterServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private AuthRateLimiterService authRateLimiterService;

	private AuthRateLimitProperties properties;

	@BeforeEach
	void setUp() {
		properties = new AuthRateLimitProperties();
		properties.setMaxRequestsPerMinute(5);
		properties.setBlockDurationMinutes(15);
		authRateLimiterService = new AuthRateLimiterService(redisTemplate, properties);
	}

	@Test
	void allowsRequestsWithinLimit() {
		when(redisTemplate.hasKey("auth:block:127.0.0.1")).thenReturn(false);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.increment("auth:rate:127.0.0.1")).thenReturn(3L);

		assertThatCode(() -> authRateLimiterService.checkAndConsume("127.0.0.1"))
			.doesNotThrowAnyException();
	}

	@Test
	void blocksWhenIpAlreadyBlocked() {
		when(redisTemplate.hasKey("auth:block:127.0.0.1")).thenReturn(true);

		assertThatThrownBy(() -> authRateLimiterService.checkAndConsume("127.0.0.1"))
			.isInstanceOf(RateLimitExceededException.class)
			.satisfies(ex -> {
				RateLimitExceededException rateLimitEx = (RateLimitExceededException) ex;
				assert rateLimitEx.isBlocked();
			});

		verify(redisTemplate, never()).opsForValue();
	}

	@Test
	void blocksAndSetsRedisKeyWhenLimitExceeded() {
		when(redisTemplate.hasKey("auth:block:127.0.0.1")).thenReturn(false);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.increment("auth:rate:127.0.0.1")).thenReturn(6L);

		assertThatThrownBy(() -> authRateLimiterService.checkAndConsume("127.0.0.1"))
			.isInstanceOf(RateLimitExceededException.class);

		verify(valueOperations).set(
			eq("auth:block:127.0.0.1"),
			eq("1"),
			eq(Duration.ofMinutes(15))
		);
	}

}
