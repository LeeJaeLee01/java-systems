package com.paymentsystem.fraud.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FraudVelocityProperties.class)
public class FraudConfig {
}
