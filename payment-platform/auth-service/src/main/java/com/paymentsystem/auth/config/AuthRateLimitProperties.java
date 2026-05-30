package com.paymentsystem.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth.rate-limit")
public class AuthRateLimitProperties {

	private int maxRequestsPerMinute = 5;

	private int blockDurationMinutes = 15;

	private String windowKeyPrefix = "auth:rate:";

	private String blockKeyPrefix = "auth:block:";

}
