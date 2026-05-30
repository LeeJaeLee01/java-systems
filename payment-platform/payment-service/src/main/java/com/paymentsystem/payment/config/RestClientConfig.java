package com.paymentsystem.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	@Bean
	public RestClient walletRestClient(@Value("${services.wallet.base-url}") String walletBaseUrl) {
		return RestClient.builder()
			.baseUrl(walletBaseUrl)
			.build();
	}

}
