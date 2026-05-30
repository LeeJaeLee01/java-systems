package com.paymentsystem.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

	private static final Duration TTL = Duration.ofHours(24);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public <T> T execute(String idempotencyKey, Class<T> responseType, Supplier<T> action) {
		String cacheKey = "idempotency:" + idempotencyKey;
		String cached = redisTemplate.opsForValue().get(cacheKey);

		if (cached != null) {
			try {
				return objectMapper.readValue(cached, responseType);
			}
			catch (Exception ex) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Stored idempotency response is invalid");
			}
		}

		T result = action.get();
		try {
			redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), TTL);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to cache idempotency response", ex);
		}
		return result;
	}

}
