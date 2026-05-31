package com.paymentsystem.fraud.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "fraud.velocity")
public class FraudVelocityProperties {

	private String keyPrefix = "fraud:velocity:user:";

	private long windowSeconds = 10;

	private long maxTransactions = 5;

	private long keyTtlBufferSeconds = 5;

}
