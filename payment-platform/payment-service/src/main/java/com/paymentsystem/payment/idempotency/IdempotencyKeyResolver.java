package com.paymentsystem.payment.idempotency;

import com.paymentsystem.common.idempotency.IdempotencyHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class IdempotencyKeyResolver {

	private IdempotencyKeyResolver() {
	}

	public static String resolveFromCurrentRequest() {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attributes == null) {
			return null;
		}
		return resolve(attributes.getRequest());
	}

	public static String resolve(HttpServletRequest request) {
		String key = request.getHeader(IdempotencyHeaders.IDEMPOTENCY_KEY);
		if (StringUtils.hasText(key)) {
			return key.trim();
		}
		key = request.getHeader(IdempotencyHeaders.X_IDEMPOTENCY_KEY);
		if (StringUtils.hasText(key)) {
			return key.trim();
		}
		return null;
	}

}
