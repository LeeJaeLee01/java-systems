package com.paymentsystem.payment.statemachine;

import com.paymentsystem.common.enums.PaymentStateEvent;
import com.paymentsystem.common.enums.TransactionStatus;
import com.paymentsystem.payment.domain.PaymentTransaction;
import com.paymentsystem.payment.exception.IllegalPaymentStateTransitionException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentLifecycleService {

	private final StateMachineFactory<TransactionStatus, PaymentStateEvent> paymentStateMachineFactory;

	public TransactionStatus transition(PaymentTransaction payment, PaymentStateEvent event) {
		TransactionStatus current = TransactionStatus.valueOf(payment.getStatus());
		TransactionStatus next = fireTransition(current, event, payment.getId());
		payment.setStatus(next.name());
		payment.setUpdatedAt(Instant.now());
		return next;
	}

	public boolean isTerminal(TransactionStatus status) {
		return status == TransactionStatus.SUCCESS
			|| status == TransactionStatus.FAILED
			|| status == TransactionStatus.FRAUD_REJECTED;
	}

	private TransactionStatus fireTransition(
		TransactionStatus current,
		PaymentStateEvent event,
		UUID paymentId
	) {
		StateMachine<TransactionStatus, PaymentStateEvent> stateMachine =
			paymentStateMachineFactory.getStateMachine(paymentId.toString());

		stateMachine.stop();
		stateMachine.getStateMachineAccessor().doWithAllRegions(access ->
			access.resetStateMachine(new DefaultStateMachineContext<>(current, null, null, null))
		);
		stateMachine.start();

		boolean accepted = stateMachine.sendEvent(MessageBuilder.withPayload(event).build());
		if (!accepted || stateMachine.getState() == null || stateMachine.getState().getId() == null) {
			throw new IllegalPaymentStateTransitionException(current, event);
		}

		stateMachine.stop();
		return stateMachine.getState().getId();
	}

}
