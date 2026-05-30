package com.paymentsystem.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientIpResolver {

	private static final String X_FORWARDED_FOR = "X-Forwarded-For";

	public String resolve(HttpServletRequest request) {
		String forwardedFor = request.getHeader(X_FORWARDED_FOR);
		if (StringUtils.hasText(forwardedFor)) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

}
