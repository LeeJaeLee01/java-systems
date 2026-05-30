package com.paymentsystem.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService {

	private final SecretKey secretKey;
	private final long accessTokenExpirationMinutes;

	public JwtService(
		@Value("${auth.jwt.secret}") String secret,
		@Value("${auth.jwt.access-token-expiration-minutes}") long accessTokenExpirationMinutes
	) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
	}

	public String generateAccessToken(UUID userId, String email) {
		Instant now = Instant.now();
		return Jwts.builder()
			.subject(userId.toString())
			.claim("email", email)
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES)))
			.signWith(secretKey)
			.compact();
	}

}
