package com.paymentsystem.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentsystem.common.constant.KafkaTopics;
import com.paymentsystem.common.dto.ApiResponse;
import com.paymentsystem.common.enums.TransactionStatus;
import com.paymentsystem.common.event.PaymentCompletedEvent;
import com.paymentsystem.payment.domain.OutboxEvent;
import com.paymentsystem.payment.domain.PaymentTransaction;
import com.paymentsystem.payment.dto.PaymentResponse;
import com.paymentsystem.payment.dto.TransferRequest;
import com.paymentsystem.payment.dto.WalletOperationPayload;
import com.paymentsystem.payment.dto.WalletServiceResponse;
import com.paymentsystem.payment.repository.OutboxEventRepository;
import com.paymentsystem.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

	private final PaymentTransactionRepository paymentTransactionRepository;
	private final OutboxEventRepository outboxEventRepository;
	private final IdempotencyService idempotencyService;
	private final RestClient walletRestClient;
	private final ObjectMapper objectMapper;

	@Transactional
	public PaymentResponse transfer(String idempotencyKey, TransferRequest request) {
		return idempotencyService.execute(idempotencyKey, PaymentResponse.class, () -> processTransfer(idempotencyKey, request));
	}

	private PaymentResponse processTransfer(String idempotencyKey, TransferRequest request) {
		paymentTransactionRepository.findByIdempotencyKey(idempotencyKey).ifPresent(existing -> {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate idempotency key");
		});

		UUID paymentId = UUID.randomUUID();
		Instant now = Instant.now();

		PaymentTransaction payment = PaymentTransaction.builder()
			.id(paymentId)
			.walletId(request.walletId())
			.userId(request.userId())
			.idempotencyKey(idempotencyKey)
			.amount(request.amount())
			.status(TransactionStatus.PROCESSING.name())
			.createdAt(now)
			.updatedAt(now)
			.build();

		paymentTransactionRepository.save(payment);

		try {
			debitWallet(request.walletId(), paymentId, request.amount());
			payment.setStatus(TransactionStatus.SUCCESS.name());
			payment.setUpdatedAt(Instant.now());
			paymentTransactionRepository.save(payment);

			PaymentResponse response = toResponse(payment);
			saveOutboxEvent(payment, response);
			return response;
		}
		catch (RuntimeException ex) {
			payment.setStatus(TransactionStatus.FAILED.name());
			payment.setUpdatedAt(Instant.now());
			paymentTransactionRepository.save(payment);
			throw ex;
		}
	}

	private void debitWallet(UUID walletId, UUID paymentId, java.math.BigDecimal amount) {
		ApiResponse<WalletServiceResponse> response = walletRestClient.post()
			.uri("/api/wallets/{walletId}/debit", walletId)
			.body(new WalletOperationPayload(paymentId, amount))
			.retrieve()
			.body(new ParameterizedTypeReference<>() {
			});

		if (response == null || !response.success()) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Wallet debit failed");
		}
	}

	private void saveOutboxEvent(PaymentTransaction payment, PaymentResponse response) {
		try {
			PaymentCompletedEvent event = new PaymentCompletedEvent(
				UUID.randomUUID(),
				payment.getId(),
				payment.getWalletId(),
				payment.getUserId(),
				payment.getAmount(),
				payment.getStatus(),
				Instant.now()
			);

			outboxEventRepository.save(OutboxEvent.builder()
				.id(UUID.randomUUID())
				.aggregateType("PAYMENT")
				.aggregateId(payment.getId())
				.eventType(KafkaTopics.PAYMENT_EVENTS)
				.payload(objectMapper.writeValueAsString(event))
				.status("PENDING")
				.createdAt(Instant.now())
				.build());
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Failed to serialize outbox payload", ex);
		}
	}

	private PaymentResponse toResponse(PaymentTransaction payment) {
		return new PaymentResponse(
			payment.getId(),
			payment.getWalletId(),
			payment.getUserId(),
			payment.getAmount(),
			TransactionStatus.valueOf(payment.getStatus()),
			payment.getCreatedAt()
		);
	}

}
