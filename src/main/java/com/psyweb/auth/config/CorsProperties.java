package com.psyweb.auth.config;

import java.net.URI;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@ConfigurationProperties(prefix = "psyweb.security.cors")
@Validated
public record CorsProperties(@NotEmpty List<@NotBlank String> allowedOrigins) {

	public CorsProperties {
		if (allowedOrigins == null || allowedOrigins.isEmpty()) {
			throw new IllegalArgumentException("CORS allowed origins must not be empty");
		}

		for (String s : allowedOrigins) {
			if (s == null || s.isBlank()) {
				throw new IllegalArgumentException("Invalid CORS origin: origin must not be blank");
			}
			URI uri = URI.create(s);
			String scheme = uri.getScheme();
			if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
				throw new IllegalArgumentException("Invalid CORS origin: unsupported or missing scheme: " + s);
			}
			String host = uri.getHost();
			if (host == null) {
				throw new IllegalArgumentException("Invalid CORS origin: host is missing: " + s);
			}
			if (uri.getRawPath() != null && !uri.getRawPath().isEmpty()) {
				throw new IllegalArgumentException("Invalid CORS origin: path is not allowed: " + s);
			}
			if (uri.getRawUserInfo() != null) {
				throw new IllegalArgumentException("Invalid CORS origin: user info is not allowed: " + s);
			}
			if (uri.getRawQuery() != null) {
				throw new IllegalArgumentException("Invalid CORS origin: query is not allowed: " + s);
			}
			if (uri.getRawFragment() != null) {
				throw new IllegalArgumentException("Invalid CORS origin: fragment is not allowed: " + s);
			}
			int port = uri.getPort();
			if (port != -1 && (port < 1 || port > 65535)) {
				throw new IllegalArgumentException("Invalid CORS origin: invalid port: " + port);
			}
			String expected = scheme + "://" + host;
			if (port != -1) {
				expected = expected + ":" + String.valueOf(port);
			} 
			if (!expected.equals(s)) {
				throw new IllegalArgumentException("Invalid CORS origin: invalid origin format: " + s);
			}
		}
		allowedOrigins = List.copyOf(allowedOrigins);
	}
}