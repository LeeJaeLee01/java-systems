package com.paymentsystem.payment.service;

import com.paymentsystem.common.enums.PaymentStateEvent;
import com.paymentsystem.common.enums.TransactionStatus;
import com.paymentsystem.payment.domain.PaymentTransaction;
import com.paymentsystem.payment.repository.PaymentTransactionRepository;
import com.paymentsystem.payment.statemachine.PaymentLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentFraudService {

	private final PaymentTransactionRepository paymentTransactionRepository;
	private final PaymentLifecycleService paymentLifecycleService;

	@Transactional
	public PaymentTransaction rejectDueToFraud(UUID paymentId) {
		PaymentTransaction payment = paymentTransactionRepository.findById(paymentId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

		TransactionStatus current = TransactionStatus.valueOf(payment.getStatus());
		if (paymentLifecycleService.isTerminal(current)) {
			throw new ResponseStatusException(
				HttpStatus.CONFLICT,
				"Payment is already in terminal state: " + current
			);
		}

		paymentLifecycleService.transition(payment, PaymentStateEvent.FRAUD_DETECT);
		return paymentTransactionRepository.save(payment);
	}

}
