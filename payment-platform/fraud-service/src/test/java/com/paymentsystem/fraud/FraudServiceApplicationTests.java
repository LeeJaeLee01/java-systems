package com.paymentsystem.fraud;

import com.paymentsystem.fraud.service.FraudDetectionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FraudServiceApplicationTests {

	@MockBean
	private FraudDetectionService fraudDetectionService;

	@Test
	void contextLoads() {
	}

}
