package com.psyweb.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.http.HttpMethod;

import com.psyweb.auth.authorization.AuthorizationPolicy;
import com.psyweb.auth.security.RoleAuthorizationManager;
import com.psyweb.user.domain.UserRole;
import com.psyweb.auth.security.ApiAccessDeniedHandler;
import com.psyweb.auth.security.ApiAuthenticationEntryPoint;
import com.psyweb.auth.security.SecurityErrorResponseWriter;
import com.psyweb.user.service.UserService;

import tools.jackson.databind.ObjectMapper;

import org.springframework.security.config.http.SessionCreationPolicy;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter,
			RoleAuthorizationManager adminAuthorizationManager, ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
			ApiAccessDeniedHandler apiAccessDeniedHandler) throws Exception {

		http.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/api/auth/register/client", "/api/auth/register/specialist")
				.permitAll().requestMatchers("/api/admin/**").access(adminAuthorizationManager).anyRequest().denyAll());

		http.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(apiAuthenticationEntryPoint)
				.accessDeniedHandler(apiAccessDeniedHandler));

		http.csrf(csrf -> csrf.disable());

		http.headers(headers -> headers
				.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
				.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")));

		http.cors(Customizer.withDefaults());

		http.formLogin(form -> form.disable());
		http.httpBasic(basic -> basic.disable());

		http.oauth2ResourceServer(oauth2 -> oauth2.authenticationEntryPoint(apiAuthenticationEntryPoint)
				.accessDeniedHandler(apiAccessDeniedHandler)
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		return http.build();
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

		authoritiesConverter.setAuthoritiesClaimName("role");
		authoritiesConverter.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
		authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

		return authenticationConverter;
	}

	@Bean
	SecurityErrorResponseWriter securityErrorResponseWriter(ObjectMapper objectMapper, Clock clock) {
		return new SecurityErrorResponseWriter(objectMapper, clock);
	}

	@Bean
	ApiAuthenticationEntryPoint apiAuthenticationEntryPoint(SecurityErrorResponseWriter responseWriter) {
		return new ApiAuthenticationEntryPoint(responseWriter);
	}

	@Bean
	ApiAccessDeniedHandler apiAccessDeniedHandler(SecurityErrorResponseWriter responseWriter) {
		return new ApiAccessDeniedHandler(responseWriter);
	}

	@Bean
	AuthorizationPolicy authorizationPolicy() {
		return new AuthorizationPolicy();
	}

	@Bean
	RoleAuthorizationManager adminAuthorizationManager(UserService userService,
			AuthorizationPolicy authorizationPolicy) {
		return new RoleAuthorizationManager(userService, authorizationPolicy, UserRole.ADMIN);
	}
}