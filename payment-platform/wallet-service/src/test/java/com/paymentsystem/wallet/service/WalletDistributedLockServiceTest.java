package com.paymentsystem.wallet.service;

import com.paymentsystem.wallet.config.WalletLockProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletDistributedLockServiceTest {

	@Mock
	private RedissonClient redissonClient;

	@Mock
	private RLock rLock;

	@InjectMocks
	private WalletDistributedLockService walletDistributedLockService;

	private WalletLockProperties properties;

	@BeforeEach
	void setUp() {
		properties = new WalletLockProperties();
		properties.setKeyPrefix("lock:wallet:");
		properties.setWaitTimeMs(3_000);
		properties.setLeaseTimeMs(10_000);
		walletDistributedLockService = new WalletDistributedLockService(redissonClient, properties);
	}

	@Test
	void executesActionWhenLockAcquired() throws Exception {
		UUID walletId = UUID.randomUUID();
		when(redissonClient.getLock("lock:wallet:" + walletId)).thenReturn(rLock);
		when(rLock.tryLock(3_000L, 10_000L, TimeUnit.MILLISECONDS)).thenReturn(true);
		when(rLock.isHeldByCurrentThread()).thenReturn(true);

		String result = walletDistributedLockService.executeWithWalletLock(walletId, () -> "ok");

		assertThat(result).isEqualTo("ok");
		verify(rLock).unlock();
	}

	@Test
	void throwsWhenLockNotAcquired() throws Exception {
		UUID walletId = UUID.randomUUID();
		when(redissonClient.getLock("lock:wallet:" + walletId)).thenReturn(rLock);
		when(rLock.tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(false);

		assertThatThrownBy(() -> walletDistributedLockService.executeWithWalletLock(walletId, () -> "ok"))
			.isInstanceOf(ResponseStatusException.class);
	}

}
