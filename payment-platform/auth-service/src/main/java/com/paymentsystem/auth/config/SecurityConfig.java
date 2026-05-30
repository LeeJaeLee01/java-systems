package com.paymentsystem.auth.config;

import com.paymentsystem.auth.security.AuthRateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableConfigurationProperties({ AuthRateLimitProperties.class, AuthCorsProperties.class })
@RequiredArgsConstructor
public class SecurityConfig {

	private final AuthRateLimitFilter authRateLimitFilter;
	private final AuthCorsProperties authCorsProperties;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.headers(headers -> headers
				.contentTypeOptions(withDefaults())
				.frameOptions(frame -> frame.deny())
				.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'"))
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/actuator/**", "/api/auth/**").permitAll()
				.anyRequest().authenticated()
			)
			.addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(authCorsProperties.getAllowedOrigins());
		configuration.setAllowedMethods(authCorsProperties.getAllowedMethods());
		configuration.setAllowedHeaders(authCorsProperties.getAllowedHeaders());
		configuration.setAllowCredentials(authCorsProperties.isAllowCredentials());

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

}
