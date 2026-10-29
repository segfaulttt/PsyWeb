package com.psyweb.auth.config;

import java.net.URI;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
public class CorsConfig {

	@Bean
	public UrlBasedCorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties,
			Environment environment) {
		if (environment.acceptsProfiles(Profiles.of("prod"))) {
			for (String origin : corsProperties.allowedOrigins()) {
				String scheme = URI.create(origin).getScheme();

				if (!scheme.equalsIgnoreCase("https")) {
					throw new IllegalArgumentException("Production CORS origin must use HTTPS: " + origin);
				}
			}
		}

		long maxAge = 3600;
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(corsProperties.allowedOrigins());
		configuration.setAllowCredentials(true);
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN"));
		configuration.setAllowedMethods(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setMaxAge(maxAge);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}
}
