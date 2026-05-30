package com.paymentsystem.payment.idempotency;

import com.paymentsystem.common.idempotency.IdempotencyKeyContext;
import com.paymentsystem.common.idempotency.Idempotent;
import com.paymentsystem.payment.exception.IdempotencyInProgressException;
import com.paymentsystem.payment.exception.IdempotencyKeyMissingException;
import com.paymentsystem.payment.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

	private final IdempotencyService idempotencyService;

	@Around("@annotation(idempotent)")
	public Object enforceIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
		String idempotencyKey = IdempotencyKeyResolver.resolveFromCurrentRequest();
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new IdempotencyKeyMissingException();
		}

		Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getReturnType();
		Optional<Object> cached = idempotencyService.findCachedResponse(idempotencyKey, returnType);
		if (cached.isPresent()) {
			return cached.get();
		}

		if (!idempotencyService.tryAcquireProcessingLock(idempotencyKey)) {
			Optional<Object> retryCached = idempotencyService.findCachedResponse(idempotencyKey, returnType);
			if (retryCached.isPresent()) {
				return retryCached.get();
			}
			throw new IdempotencyInProgressException();
		}

		try {
			IdempotencyKeyContext.set(idempotencyKey);
			Object result = joinPoint.proceed();
			idempotencyService.saveResponse(idempotencyKey, result);
			return result;
		}
		catch (Throwable ex) {
			idempotencyService.releaseProcessingLock(idempotencyKey);
			throw ex;
		}
		finally {
			IdempotencyKeyContext.clear();
		}
	}

}
