package com.paymentsystem.auth.controller;

import com.paymentsystem.auth.dto.AuthResponse;
import com.paymentsystem.auth.dto.LoginRequest;
import com.paymentsystem.auth.dto.RegisterRequest;
import com.paymentsystem.auth.service.AuthService;
import com.paymentsystem.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@GetMapping("/health")
	public ApiResponse<String> health() {
		return ApiResponse.ok("auth-service is running");
	}

	@PostMapping("/register")
	public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ApiResponse.ok("Registered successfully", authService.register(request));
	}

	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.ok("Logged in successfully", authService.login(request));
	}

}
