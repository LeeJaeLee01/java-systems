package com.paymentsystem.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "notification.executor")
public class NotificationExecutorProperties {

	private int corePoolSize = 4;

	private int maxPoolSize = 8;

	private int queueCapacity = 100;

	private String threadNamePrefix = "notification-";

}
