package com.paymentsystem.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentsystem.payment.config.IdempotencyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final IdempotencyProperties properties;

	public Optional<Object> findCachedResponse(String idempotencyKey, Class<?> responseType) {
		String cached = redisTemplate.opsForValue().get(responseKey(idempotencyKey));
		if (cached == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(cached, responseType));
		}
		catch (Exception ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Stored idempotency response is invalid");
		}
	}

	public boolean tryAcquireProcessingLock(String idempotencyKey) {
		Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
			processingKey(idempotencyKey),
			"1",
			processingTtl()
		);
		return Boolean.TRUE.equals(acquired);
	}

	public void saveResponse(String idempotencyKey, Object response) {
		try {
			redisTemplate.opsForValue().set(
				responseKey(idempotencyKey),
				objectMapper.writeValueAsString(response),
				responseTtl()
			);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to cache idempotency response", ex);
		}
		finally {
			releaseProcessingLock(idempotencyKey);
		}
	}

	public void releaseProcessingLock(String idempotencyKey) {
		redisTemplate.delete(processingKey(idempotencyKey));
	}

	private String responseKey(String idempotencyKey) {
		return properties.getResponseKeyPrefix() + idempotencyKey;
	}

	private String processingKey(String idempotencyKey) {
		return properties.getProcessingKeyPrefix() + idempotencyKey;
	}

	private Duration responseTtl() {
		return Duration.ofHours(properties.getResponseTtlHours());
	}

	private Duration processingTtl() {
		return Duration.ofMinutes(properties.getProcessingTtlMinutes());
	}

}
