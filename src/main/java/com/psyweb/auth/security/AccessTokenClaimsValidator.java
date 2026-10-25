package com.psyweb.auth.security;

import org.springframework.security.oauth2.jwt.Jwt;

import com.psyweb.auth.config.JwtProperties;
import com.psyweb.user.domain.UserRole;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;

public class AccessTokenClaimsValidator implements OAuth2TokenValidator<Jwt> {
	private final Clock clock;
	private final JwtProperties jwtProperties;

	public AccessTokenClaimsValidator(Clock clock, JwtProperties jwtProperties) {
		this.clock = clock;
		this.jwtProperties = jwtProperties;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		// sub
		String subject = jwt.getSubject();
		if (subject == null || subject.isBlank()) {
			OAuth2Error error = new OAuth2Error("invalid_token", "Invalid JWT subject", null);
			return OAuth2TokenValidatorResult.failure(error);
		}
		try {
			long sub = Long.parseLong(subject);
			if (sub <= 0) {
				OAuth2Error error = new OAuth2Error("invalid_token", "Invalid JWT subject", null);
				return OAuth2TokenValidatorResult.failure(error);
			}
		} catch (NumberFormatException e) {
			OAuth2Error error = new OAuth2Error("invalid_token", "Invalid JWT subject", null);
			return OAuth2TokenValidatorResult.failure(error);
		}

		//role
		String role = jwt.getClaimAsString("role");
		if (role == null || role.isBlank()) {
			OAuth2Error error = new OAuth2Error("invalid_token", "Invalid JWT role", null);
			return OAuth2TokenValidatorResult.failure(error);
		}
		try {
			UserRole.valueOf(role);
		} catch (IllegalArgumentException e) {
			OAuth2Error error = new OAuth2Error("invalid_token", "Invalid JWT role", null);
			return OAuth2TokenValidatorResult.failure(error);
		}

		// iat && eat
		Instant now = Instant.now(clock);
		Instant issuedAt = jwt.getIssuedAt();
		Instant expiresAt = jwt.getExpiresAt();
		if (issuedAt == null || expiresAt == null) {
			OAuth2Error error = new OAuth2Error("invalid_token", "Invalid JWT time", null);
			return OAuth2TokenValidatorResult.failure(error);
		}
		if (!expiresAt.isAfter(issuedAt)) {
			OAuth2Error error = new OAuth2Error("invalid_token", "Invalid JWT time", null);
			return OAuth2TokenValidatorResult.failure(error);
		}
		if (Duration.between(issuedAt, expiresAt).compareTo(jwtProperties.accessTokenTtl()) > 0) {
			OAuth2Error error = new OAuth2Error("invalid_token", "Invalid JWT time", null);
			return OAuth2TokenValidatorResult.failure(error);
		}
		if (issuedAt.isAfter(now.plus(Duration.ofSeconds(60)))) {
			OAuth2Error error = new OAuth2Error("invalid_token", "Invalid JWT time", null);
			return OAuth2TokenValidatorResult.failure(error);
		}
		
		// kid
		Map<String, Object> headers = jwt.getHeaders();
		Object keyId = headers.get("kid");
		if (!jwtProperties.keyId().equals(keyId)) {
			OAuth2Error error = new OAuth2Error("invalid_token", "Invalid JWT key ID", null);
			return OAuth2TokenValidatorResult.failure(error);
		}

		return OAuth2TokenValidatorResult.success();
	}
}
