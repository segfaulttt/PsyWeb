
package com.psyweb.auth.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CorsPropertiesTest {

	@ParameterizedTest
	@ValueSource(strings = { "http://localhost:3000", "http://localhost", "http://127.0.0.1:8080", "https://psyweb.com",
			"https://psyweb.com:8443", "http://[::1]:3000", "HTTP://localhost:3000"})
	void shouldAcceptValidOrigins(String origin) {
		CorsProperties properties = new CorsProperties(List.of(origin));

		assertEquals(List.of(origin), properties.allowedOrigins());
	}

	@ParameterizedTest
	@ValueSource(strings = { "*", "psyweb.com", "ftp://psyweb.com", "http://", "http://localhost:",
			"http://localhost:abc", "http://localhost:0", "http://localhost:65536", "http://localhost:03000",
			"https://psyweb.com/", "https://psyweb.com/login", "https://psyweb.com?foo=bar", "https://psyweb.com?",
			"https://psyweb.com#section", "https://psyweb.com#", "https://user:password@psyweb.com",
			"https://@psyweb.com"})
	void shouldRejectInvalidOrigins(String origin) {
		assertThrows(IllegalArgumentException.class, () -> new CorsProperties(List.of(origin)));
	}

	@ParameterizedTest
	@NullAndEmptySource
	void shouldRejectNullOrEmptyOriginsList(List<String> origins) {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new CorsProperties(origins));

		assertEquals("CORS allowed origins must not be empty", exception.getMessage());
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "   ", "\t" })
	void shouldRejectBlankOrigin(String origin) {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new CorsProperties(List.of(origin)));

		assertEquals("Invalid CORS origin: origin must not be blank", exception.getMessage());
	}

	@Test
	void shouldRejectNullOriginElement() {
		List<String> origins = new ArrayList<>();
		origins.add(null);

		assertThrows(IllegalArgumentException.class, () -> new CorsProperties(origins));
	}

	@Test
	void shouldRejectInvalidOriginAmongValidOrigins() {
		List<String> origins = List.of("http://localhost:3000", "https://psyweb.com", "ftp://invalid.com");

		assertThrows(IllegalArgumentException.class, () -> new CorsProperties(origins));
	}

	@Test
	void shouldPreserveMultipleValidOrigins() {
		List<String> origins = List.of("http://localhost:3000", "https://psyweb.com");

		CorsProperties properties = new CorsProperties(origins);

		assertEquals(origins, properties.allowedOrigins());
	}

	@Test
	void shouldCreateDefensiveCopyOfAllowedOrigins() {
		List<String> origins = new ArrayList<>();
		origins.add("http://localhost:3000");

		CorsProperties properties = new CorsProperties(origins);

		origins.add("https://unexpected.com");

		assertEquals(List.of("http://localhost:3000"), properties.allowedOrigins());
	}

	@Test
	void shouldExposeUnmodifiableAllowedOrigins() {
		CorsProperties properties = new CorsProperties(List.of("http://localhost:3000"));

		assertThrows(UnsupportedOperationException.class,
				() -> properties.allowedOrigins().add("https://unexpected.com"));
	}
}