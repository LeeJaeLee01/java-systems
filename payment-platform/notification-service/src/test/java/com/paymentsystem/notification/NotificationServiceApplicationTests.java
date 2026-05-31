package com.paymentsystem.notification;

import com.paymentsystem.notification.kafka.PaymentCompletedEventListener;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceApplicationTests {

	@MockBean
	private PaymentCompletedEventListener paymentCompletedEventListener;

	@Test
	void contextLoads() {
	}

}
