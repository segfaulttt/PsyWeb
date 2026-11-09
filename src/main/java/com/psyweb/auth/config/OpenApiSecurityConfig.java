package com.psyweb.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("local")
public class OpenApiSecurityConfig {

	@Bean
	@Order(1)
	SecurityFilterChain openApiSecurityFilterChain(HttpSecurity http) throws Exception {

		return http.securityMatcher("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
	}
}