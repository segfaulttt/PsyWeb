package com.psyweb.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import com.psyweb.auth.security.ActiveUserAuthorizationManager;
import com.psyweb.auth.security.ApiAccessDeniedHandler;
import com.psyweb.auth.security.ApiAuthenticationEntryPoint;
import com.psyweb.auth.security.SecurityErrorResponseWriter;
import com.psyweb.user.service.UserService;

import tools.jackson.databind.ObjectMapper;

import org.springframework.security.config.http.SessionCreationPolicy;
import static org.springframework.security.authorization.AuthorizationManagers.allOf;

import java.time.Clock;

import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter,
			ActiveUserAuthorizationManager activeUserAuthorizationManager,
			ApiAuthenticationEntryPoint apiAuthenticationEntryPoint, ApiAccessDeniedHandler apiAccessDeniedHandler)
			throws Exception {

		http.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/api/auth/register/client", "/api/auth/register/specialist")
				.permitAll().requestMatchers("/api/admin/**")
				.access(allOf(activeUserAuthorizationManager, hasRole("ADMIN"))).anyRequest().denyAll());
		
		http.exceptionHandling(exceptions -> exceptions
		        .authenticationEntryPoint(apiAuthenticationEntryPoint)
		        .accessDeniedHandler(apiAccessDeniedHandler));

		http.csrf(csrf -> csrf.ignoringRequestMatchers(
				PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/auth/register/client"),
				PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/auth/register/specialist")));

		http.cors(Customizer.withDefaults());
		
		http.formLogin(form -> form.disable());
		http.httpBasic(basic -> basic.disable());

		http.oauth2ResourceServer(oauth2 -> oauth2
		        .authenticationEntryPoint(apiAuthenticationEntryPoint)
		        .accessDeniedHandler(apiAccessDeniedHandler)
		        .jwt(jwt -> jwt
		                .jwtAuthenticationConverter(jwtAuthenticationConverter)));

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
	ActiveUserAuthorizationManager activeUserAuthorizationManager(UserService userService) {
		return new ActiveUserAuthorizationManager(userService);
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
}