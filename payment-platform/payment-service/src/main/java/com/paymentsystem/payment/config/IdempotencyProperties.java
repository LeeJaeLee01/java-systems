package com.paymentsystem.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "payment.idempotency")
public class IdempotencyProperties {

	private String responseKeyPrefix = "idempotency:response:";

	private String processingKeyPrefix = "idempotency:processing:";

	private long responseTtlHours = 24;

	private long processingTtlMinutes = 5;

}
