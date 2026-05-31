package com.paymentsystem.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Getter
@Setter
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

	private String apiKey = "";

	private String webhookSecret = "";

	private long webhookToleranceSeconds = 300;

	public boolean hasApiKey() {
		return StringUtils.hasText(apiKey);
	}

	public boolean hasWebhookSecret() {
		return StringUtils.hasText(webhookSecret);
	}

}
