
package com.psyweb.auth.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

class CorsConfigTest {

	private final CorsConfig corsConfig = new CorsConfig();

	@Test
	void shouldAllowHttpsOriginInProduction() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("prod");

		CorsProperties properties = new CorsProperties(List.of("https://example.com"));

		UrlBasedCorsConfigurationSource source = corsConfig.corsConfigurationSource(properties, environment);

		assertNotNull(source);
	}

	@Test
	void shouldRejectHttpOriginInProduction() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("prod");

		CorsProperties properties = new CorsProperties(List.of("http://example.com"));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> corsConfig.corsConfigurationSource(properties, environment));

		assertTrue(exception.getMessage().contains("HTTPS"));
	}

	@Test
	void shouldAllowHttpOriginInTestProfile() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("test");

		CorsProperties properties = new CorsProperties(List.of("http://localhost:3000"));

		UrlBasedCorsConfigurationSource source = corsConfig.corsConfigurationSource(properties, environment);

		assertNotNull(source);
	}
}