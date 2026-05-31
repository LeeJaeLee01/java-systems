package com.paymentsystem.payment.controller;

import com.paymentsystem.common.dto.ApiResponse;
import com.paymentsystem.common.idempotency.Idempotent;
import com.paymentsystem.payment.dto.PaymentResponse;
import com.paymentsystem.payment.dto.TransferRequest;
import com.paymentsystem.payment.service.PaymentFraudService;
import com.paymentsystem.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;
	private final PaymentFraudService paymentFraudService;

	@GetMapping("/health")
	public ApiResponse<String> health() {
		return ApiResponse.ok("payment-service is running");
	}

	@Idempotent
	@PostMapping("/transfer")
	public ApiResponse<PaymentResponse> transfer(@Valid @RequestBody TransferRequest request) {
		return ApiResponse.ok("Transfer processed", paymentService.transfer(request));
	}

	@PostMapping("/{paymentId}/fraud-reject")
	public ApiResponse<String> rejectDueToFraud(@PathVariable UUID paymentId) {
		paymentFraudService.rejectDueToFraud(paymentId);
		return ApiResponse.ok("Payment marked as FRAUD_REJECTED");
	}

}
