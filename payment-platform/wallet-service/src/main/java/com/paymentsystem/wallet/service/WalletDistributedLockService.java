package com.paymentsystem.wallet.service;

import com.paymentsystem.wallet.config.WalletLockProperties;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class WalletDistributedLockService {

	private final RedissonClient redissonClient;
	private final WalletLockProperties lockProperties;

	public <T> T executeWithWalletLock(UUID walletId, Supplier<T> action) {
		String lockKey = lockProperties.getKeyPrefix() + walletId;
		RLock lock = redissonClient.getLock(lockKey);

		boolean acquired;
		try {
			acquired = lock.tryLock(
				lockProperties.getWaitTimeMs(),
				lockProperties.getLeaseTimeMs(),
				TimeUnit.MILLISECONDS
			);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Wallet lock interrupted");
		}

		if (!acquired) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Wallet is busy, please retry");
		}

		try {
			return action.get();
		}
		finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

}
