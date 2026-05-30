package com.paymentsystem.wallet.service;

import com.paymentsystem.wallet.config.WalletCacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RedisWalletBalanceCache {

	private static final DefaultRedisScript<Long> ATOMIC_DEBIT_SCRIPT = new DefaultRedisScript<>(
		"""
			local wallet_key = KEYS[1]
			local tx_amount = tonumber(ARGV[1])
			local current_balance = tonumber(redis.call('get', wallet_key) or "0")
			if current_balance >= tx_amount then
			    local new_balance = current_balance - tx_amount
			    redis.call('set', wallet_key, tostring(new_balance))
			    return new_balance
			else
			    return -1
			end
			""",
		Long.class
	);

	private static final DefaultRedisScript<Long> ATOMIC_CREDIT_SCRIPT = new DefaultRedisScript<>(
		"""
			local wallet_key = KEYS[1]
			local tx_amount = tonumber(ARGV[1])
			local current_balance = tonumber(redis.call('get', wallet_key) or "0")
			local new_balance = current_balance + tx_amount
			redis.call('set', wallet_key, tostring(new_balance))
			return new_balance
			""",
		Long.class
	);

	private final StringRedisTemplate redisTemplate;
	private final WalletCacheProperties cacheProperties;

	public void ensureLoaded(UUID walletId, Supplier<BigDecimal> balanceSupplier) {
		if (!cacheProperties.isEnabled()) {
			return;
		}
		String key = balanceKey(walletId);
		if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
			return;
		}
		syncBalance(walletId, balanceSupplier.get());
	}

	public boolean tryAtomicDebit(UUID walletId, BigDecimal amount) {
		if (!cacheProperties.isEnabled()) {
			return true;
		}
		Long result = redisTemplate.execute(
			ATOMIC_DEBIT_SCRIPT,
			List.of(balanceKey(walletId)),
			amount.toPlainString()
		);
		return result != null && result >= 0;
	}

	public void atomicCredit(UUID walletId, BigDecimal amount) {
		if (!cacheProperties.isEnabled()) {
			return;
		}
		redisTemplate.execute(
			ATOMIC_CREDIT_SCRIPT,
			List.of(balanceKey(walletId)),
			amount.toPlainString()
		);
	}

	public void syncBalance(UUID walletId, BigDecimal balance) {
		if (!cacheProperties.isEnabled()) {
			return;
		}
		redisTemplate.opsForValue().set(balanceKey(walletId), balance.toPlainString());
	}

	private String balanceKey(UUID walletId) {
		return cacheProperties.getBalanceKeyPrefix() + walletId;
	}

}
