package com.paymentsystem.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth.cors")
public class AuthCorsProperties {

	private List<String> allowedOrigins = List.of(
		"http://localhost:3000",
		"http://localhost:8080"
	);

	private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

	private List<String> allowedHeaders = List.of("*");

	private boolean allowCredentials = true;

}
