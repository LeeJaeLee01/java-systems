package com.paymentsystem.fraud.service;

import com.paymentsystem.fraud.config.FraudVelocityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Sliding-window velocity check backed by Redis Sorted Sets.
 * Score = event timestamp (epoch millis), member = unique event id.
 */
@Service
@RequiredArgsConstructor
public class SlidingWindowVelocityService {

	private final StringRedisTemplate redisTemplate;
	private final FraudVelocityProperties properties;

	public VelocityCheckResult checkUserVelocity(UUID userId, UUID eventId, Instant occurredAt) {
		String key = properties.getKeyPrefix() + userId;
		long eventTimeMs = occurredAt.toEpochMilli();
		long windowStartMs = eventTimeMs - (properties.getWindowSeconds() * 1000L);

		ZSetOperations<String, String> zSet = redisTemplate.opsForZSet();
		zSet.add(key, eventId.toString(), eventTimeMs);
		zSet.removeRangeByScore(key, 0, windowStartMs);

		Long count = zSet.zCard(key);
		long transactionCount = count != null ? count : 0L;
		redisTemplate.expire(
			key,
			Duration.ofSeconds(properties.getWindowSeconds() + properties.getKeyTtlBufferSeconds())
		);

		boolean exceeded = transactionCount > properties.getMaxTransactions();
		return new VelocityCheckResult(
			userId,
			transactionCount,
			properties.getWindowSeconds(),
			properties.getMaxTransactions(),
			exceeded
		);
	}

}
