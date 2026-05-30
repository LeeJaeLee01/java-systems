package com.paymentsystem.notification.controller;

import com.paymentsystem.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	@GetMapping("/health")
	public ApiResponse<String> health() {
		return ApiResponse.ok("notification-service is running");
	}

}
