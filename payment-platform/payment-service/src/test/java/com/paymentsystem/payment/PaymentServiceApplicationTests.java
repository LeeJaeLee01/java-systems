package com.paymentsystem.payment;

import com.paymentsystem.payment.service.IdempotencyService;
import com.paymentsystem.payment.service.OutboxPublisher;
import com.paymentsystem.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceApplicationTests {

	@MockBean
	private PaymentService paymentService;

	@MockBean
	private IdempotencyService idempotencyService;

	@MockBean
	private OutboxPublisher outboxPublisher;

	@Test
	void contextLoads() {
	}

}
