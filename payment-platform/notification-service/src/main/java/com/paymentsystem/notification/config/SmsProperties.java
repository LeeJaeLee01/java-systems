package com.paymentsystem.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "notification.sms")
public class SmsProperties {

	private boolean enabled = false;

	private String region = "us-east-1";

	private String defaultCountryCode = "+1";

}
