package com.paymentsystem.wallet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "wallet.cache")
public class WalletCacheProperties {

	private boolean enabled = true;

	private String balanceKeyPrefix = "wallet:balance:";

}
