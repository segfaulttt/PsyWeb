package com.psyweb.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/api/auth/register/client", "/api/auth/register/specialist")
				.permitAll().anyRequest().denyAll());

		http.csrf(csrf -> csrf.ignoringRequestMatchers(
				PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/auth/register/client"),
				PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/auth/register/specialist")));

		http.formLogin(form -> form.disable());
		http.httpBasic(basic -> basic.disable());

		return http.build();
	}
}