package com.psyweb.auth.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {
	
	@Bean
	public UrlBasedCorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
		long maxAge = 3600;
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(corsProperties.allowedOrigins());
		configuration.setAllowCredentials(true);
		configuration.setAllowedHeaders(List.of(
				"Authorization",
				"Content-Type",
				"X-CSRF-TOKEN"));
		configuration.setAllowedMethods(List.of(
				"GET", 
				"HEAD", 
				"POST", 
				"PUT", 
				"PATCH", 
				"DELETE", 
				"OPTIONS"));
		configuration.setMaxAge(maxAge);
		
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}
}
