package com.paymentsystem.payment.controller;

import com.paymentsystem.common.dto.ApiResponse;
import com.paymentsystem.payment.dto.PaymentResponse;
import com.paymentsystem.payment.dto.TransferRequest;
import com.paymentsystem.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	@GetMapping("/health")
	public ApiResponse<String> health() {
		return ApiResponse.ok("payment-service is running");
	}

	@PostMapping("/transfer")
	public ApiResponse<PaymentResponse> transfer(
		@RequestHeader("Idempotency-Key") String idempotencyKey,
		@Valid @RequestBody TransferRequest request
	) {
		return ApiResponse.ok("Transfer processed", paymentService.transfer(idempotencyKey, request));
	}

}
