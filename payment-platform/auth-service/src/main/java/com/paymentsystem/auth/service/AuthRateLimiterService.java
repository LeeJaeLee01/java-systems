package com.paymentsystem.auth.service;

import com.paymentsystem.auth.config.AuthRateLimitProperties;
import com.paymentsystem.auth.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthRateLimiterService {

	private final StringRedisTemplate redisTemplate;
	private final AuthRateLimitProperties properties;

	public void checkAndConsume(String clientIp) {
		String blockKey = properties.getBlockKeyPrefix() + clientIp;
		if (Boolean.TRUE.equals(redisTemplate.hasKey(blockKey))) {
			throw RateLimitExceededException.blocked();
		}

		String rateKey = properties.getWindowKeyPrefix() + clientIp;
		Long currentCount = redisTemplate.opsForValue().increment(rateKey);
		if (currentCount != null && currentCount == 1L) {
			redisTemplate.expire(rateKey, Duration.ofMinutes(1));
		}

		if (currentCount != null && currentCount > properties.getMaxRequestsPerMinute()) {
			redisTemplate.opsForValue().set(
				blockKey,
				"1",
				Duration.ofMinutes(properties.getBlockDurationMinutes())
			);
			throw RateLimitExceededException.exceeded();
		}
	}

}
