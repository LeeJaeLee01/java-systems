package com.paymentsystem.wallet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "wallet.lock")
public class WalletLockProperties {

	private String keyPrefix = "lock:wallet:";

	private long waitTimeMs = 3_000;

	private long leaseTimeMs = 10_000;

}
