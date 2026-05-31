package com.paymentsystem.payment.exception;

import com.paymentsystem.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentStateExceptionHandler {

	@ExceptionHandler(IllegalPaymentStateTransitionException.class)
	public ResponseEntity<ApiResponse<Void>> handleIllegalTransition(IllegalPaymentStateTransitionException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(ApiResponse.error(ex.getMessage()));
	}

}
