package com.psyweb.auth.web.cookie;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieFactory {
	private static final String COOKIE_NAME = "__Host-refreshToken";
	private static final String COOKIE_PATH = "/";
	private static final String SAME_SITE = "Strict";

	private final Clock clock;

	public RefreshCookieFactory(Clock clock) {
		this.clock = clock;
	}

	public ResponseCookie create(String refreshToken, Instant refreshExpiresAt) {
		Duration remaining = Duration.between(Instant.now(clock), refreshExpiresAt);

		if (remaining.isNegative()) {
			remaining = Duration.ZERO;
		}
		ResponseCookie response = ResponseCookie.from(COOKIE_NAME, refreshToken).httpOnly(true).secure(true)
				.sameSite(SAME_SITE).path(COOKIE_PATH).maxAge(remaining).build();
		return response;
	}

	public ResponseCookie clear() {
		return ResponseCookie.from(COOKIE_NAME, "").httpOnly(true).secure(true).sameSite(SAME_SITE).path(COOKIE_PATH)
				.maxAge(Duration.ZERO).build();
	}
}
