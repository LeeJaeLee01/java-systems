package com.paymentsystem.auth.service;

import com.paymentsystem.auth.domain.User;
import com.paymentsystem.auth.dto.AuthResponse;
import com.paymentsystem.auth.dto.LoginRequest;
import com.paymentsystem.auth.dto.RegisterRequest;
import com.paymentsystem.auth.repository.UserRepository;
import com.paymentsystem.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
		}

		Instant now = Instant.now();
		User user = User.builder()
			.id(UUID.randomUUID())
			.email(request.email())
			.passwordHash(passwordEncoder.encode(request.password()))
			.fullName(request.fullName())
			.status("ACTIVE")
			.createdAt(now)
			.updatedAt(now)
			.build();

		userRepository.save(user);
		return buildAuthResponse(user);
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}

		return buildAuthResponse(user);
	}

	private AuthResponse buildAuthResponse(User user) {
		return new AuthResponse(
			user.getId(),
			user.getEmail(),
			jwtService.generateAccessToken(user.getId(), user.getEmail())
		);
	}

}
