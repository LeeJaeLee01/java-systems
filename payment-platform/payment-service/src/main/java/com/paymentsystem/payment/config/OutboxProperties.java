package com.paymentsystem.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "payment.outbox")
public class OutboxProperties {

	private long pollIntervalMs = 5000;

	private int batchSize = 50;

	private int maxRetries = 5;

	private long initialRetryDelaySeconds = 30;

	private long publishTimeoutSeconds = 10;

	private long claimStaleSeconds = 300;

}
