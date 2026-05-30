package com.paymentsystem.payment.exception;

import com.paymentsystem.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class IdempotencyExceptionHandler {

	@ExceptionHandler(IdempotencyKeyMissingException.class)
	public ResponseEntity<ApiResponse<Void>> handleMissingKey(IdempotencyKeyMissingException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error(ex.getMessage()));
	}

	@ExceptionHandler(IdempotencyInProgressException.class)
	public ResponseEntity<ApiResponse<Void>> handleInProgress(IdempotencyInProgressException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(ApiResponse.error(ex.getMessage()));
	}

}
