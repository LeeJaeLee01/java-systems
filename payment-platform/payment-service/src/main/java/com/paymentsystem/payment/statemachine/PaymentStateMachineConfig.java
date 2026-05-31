package com.paymentsystem.payment.statemachine;

import com.paymentsystem.common.enums.PaymentStateEvent;
import com.paymentsystem.common.enums.TransactionStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "paymentStateMachineFactory")
public class PaymentStateMachineConfig extends StateMachineConfigurerAdapter<TransactionStatus, PaymentStateEvent> {

	@Override
	public void configure(StateMachineStateConfigurer<TransactionStatus, PaymentStateEvent> states) throws Exception {
		states.withStates()
			.initial(TransactionStatus.PENDING)
			.states(EnumSet.allOf(TransactionStatus.class))
			.end(TransactionStatus.SUCCESS)
			.end(TransactionStatus.FAILED)
			.end(TransactionStatus.FRAUD_REJECTED);
	}

	@Override
	public void configure(StateMachineTransitionConfigurer<TransactionStatus, PaymentStateEvent> transitions)
		throws Exception {
		transitions
			.withExternal()
			.source(TransactionStatus.PENDING).target(TransactionStatus.PROCESSING)
			.event(PaymentStateEvent.START_PROCESSING)
			.and()
			.withExternal()
			.source(TransactionStatus.PENDING).target(TransactionStatus.FAILED)
			.event(PaymentStateEvent.FAIL)
			.and()
			.withExternal()
			.source(TransactionStatus.PROCESSING).target(TransactionStatus.SUCCESS)
			.event(PaymentStateEvent.COMPLETE)
			.and()
			.withExternal()
			.source(TransactionStatus.PROCESSING).target(TransactionStatus.FAILED)
			.event(PaymentStateEvent.FAIL)
			.and()
			.withExternal()
			.source(TransactionStatus.PROCESSING).target(TransactionStatus.FRAUD_REJECTED)
			.event(PaymentStateEvent.FRAUD_DETECT);
	}

}
