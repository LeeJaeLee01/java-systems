package com.paymentsystem.wallet;

import com.paymentsystem.wallet.service.RedisWalletBalanceCache;
import com.paymentsystem.wallet.service.WalletDistributedLockService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class WalletServiceApplicationTests {

	@MockBean
	private RedissonClient redissonClient;

	@MockBean
	private WalletDistributedLockService walletDistributedLockService;

	@MockBean
	private RedisWalletBalanceCache redisWalletBalanceCache;

	@Test
	void contextLoads() {
	}

}
