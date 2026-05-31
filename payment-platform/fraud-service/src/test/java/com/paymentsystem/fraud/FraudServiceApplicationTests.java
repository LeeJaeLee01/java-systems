package com.paymentsystem.fraud;

import com.paymentsystem.fraud.kafka.PaymentEventListener;
import com.paymentsystem.fraud.service.FraudDetectionService;
import com.paymentsystem.fraud.service.SlidingWindowVelocityService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FraudServiceApplicationTests {

	@MockBean
	private PaymentEventListener paymentEventListener;

	@MockBean
	private FraudDetectionService fraudDetectionService;

	@MockBean
	private SlidingWindowVelocityService slidingWindowVelocityService;

	@Test
	void contextLoads() {
	}

}
