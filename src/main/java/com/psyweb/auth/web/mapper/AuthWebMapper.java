package com.psyweb.auth.web.mapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.psyweb.auth.service.dto.AuthenticationResult;
import com.psyweb.auth.web.dto.response.AuthenticationResponse;

@Component
public class AuthWebMapper {
	private static final String TOKEN_TYPE = "Bearer";
	private final Clock clock;

	public AuthWebMapper(Clock clock) {
		this.clock = clock;
	}

	public AuthenticationResponse toAuthenticationResponse(AuthenticationResult result) {
		long expiresIn = Math.max(0, Duration.between(Instant.now(clock), result.accessExpiresAt()).toSeconds());
		return new AuthenticationResponse(result.accessToken(), TOKEN_TYPE, expiresIn);
	}
}
