package com.paymentsystem.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentsystem.auth.exception.RateLimitExceededException;
import com.paymentsystem.auth.service.AuthRateLimiterService;
import com.paymentsystem.common.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

	private static final Set<String> RATE_LIMITED_PATHS = Set.of(
		"/api/auth/login",
		"/api/auth/register"
	);

	private final AuthRateLimiterService authRateLimiterService;
	private final ClientIpResolver clientIpResolver;
	private final ObjectMapper objectMapper;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (!"POST".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		return !RATE_LIMITED_PATHS.contains(request.getRequestURI());
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		try {
			authRateLimiterService.checkAndConsume(clientIpResolver.resolve(request));
			filterChain.doFilter(request, response);
		}
		catch (RateLimitExceededException ex) {
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setHeader("Retry-After", ex.isBlocked() ? "900" : "60");
			objectMapper.writeValue(
				response.getWriter(),
				ApiResponse.error(ex.getMessage())
			);
		}
	}

}
