package com.paymentsystem.auth;

import com.paymentsystem.auth.service.AuthRateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

	@MockBean
	private AuthRateLimiterService authRateLimiterService;

	@Test
	void contextLoads() {
	}

}
