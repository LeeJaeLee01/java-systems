package com.paymentsystem.fraud.controller;

import com.paymentsystem.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fraud")
public class FraudController {

	@GetMapping("/health")
	public ApiResponse<String> health() {
		return ApiResponse.ok("fraud-service is running");
	}

}
