package com.paymentsystem.payment.service;

import com.paymentsystem.common.dto.ApiResponse;
import com.paymentsystem.common.enums.PaymentStateEvent;
import com.paymentsystem.common.enums.TransactionStatus;
import com.paymentsystem.common.idempotency.IdempotencyKeyContext;
import com.paymentsystem.payment.domain.PaymentTransaction;
import com.paymentsystem.payment.dto.PaymentResponse;
import com.paymentsystem.payment.dto.TransferRequest;
import com.paymentsystem.payment.dto.WalletOperationPayload;
import com.paymentsystem.payment.dto.WalletServiceResponse;
import com.paymentsystem.payment.repository.PaymentTransactionRepository;
import com.paymentsystem.payment.statemachine.PaymentLifecycleService;
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
	private final OutboxService outboxService;
	private final PaymentLifecycleService paymentLifecycleService;
	private final RestClient walletRestClient;

	@Transactional
	public PaymentResponse transfer(TransferRequest request) {
		return processTransfer(IdempotencyKeyContext.require(), request);
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
			.status(TransactionStatus.PENDING.name())
			.createdAt(now)
			.updatedAt(now)
			.build();

		paymentTransactionRepository.save(payment);
		paymentLifecycleService.transition(payment, PaymentStateEvent.START_PROCESSING);
		paymentTransactionRepository.save(payment);

		try {
			debitWallet(request.walletId(), paymentId, request.amount());
			paymentLifecycleService.transition(payment, PaymentStateEvent.COMPLETE);
			paymentTransactionRepository.save(payment);
			outboxService.enqueuePaymentCompleted(payment);
			return toResponse(payment);
		}
		catch (RuntimeException ex) {
			if (!paymentLifecycleService.isTerminal(TransactionStatus.valueOf(payment.getStatus()))) {
				paymentLifecycleService.transition(payment, PaymentStateEvent.FAIL);
				paymentTransactionRepository.save(payment);
			}
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
