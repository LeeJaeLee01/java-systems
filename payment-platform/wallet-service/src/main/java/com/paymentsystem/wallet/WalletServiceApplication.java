package com.paymentsystem.wallet;

import com.paymentsystem.wallet.config.WalletCacheProperties;
import com.paymentsystem.wallet.config.WalletLockProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableConfigurationProperties({ WalletLockProperties.class, WalletCacheProperties.class })
public class WalletServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WalletServiceApplication.class, args);
	}

}
