package com.psyweb.auth.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.psyweb.auth.config.JwtProperties;
import com.psyweb.auth.service.AccessTokenService;
import com.psyweb.auth.service.AuthenticatedUser;
import com.psyweb.user.domain.UserRole;

@Service
public class AccessTokenServiceImpl implements AccessTokenService {
	private final JwtProperties jwtProperties;
	private final Clock clock;
	private final JwtEncoder jwtEncoder;
	private final JwtDecoder jwtDecoder;

	public AccessTokenServiceImpl(JwtProperties jwtProperties, Clock clock, JwtEncoder jwtEncoder,
			JwtDecoder jwtDecoder) {
		this.jwtProperties = jwtProperties;
		this.clock = clock;
		this.jwtEncoder = jwtEncoder;
		this.jwtDecoder = jwtDecoder;
	}

	@Override
	public String generateAccessToken(Long userId, UserRole role) {
		if (userId == null || userId <= 0 || role == null) {
			throw new IllegalArgumentException("Invalid access token arguments");
		}

		Instant now = Instant.now(clock);
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer(jwtProperties.issuer()).subject(String.valueOf(userId))
				.audience(List.of(jwtProperties.audience())).claim("role", role.name()).issuedAt(now)
				.expiresAt(now.plus(jwtProperties.accessTokenTtl())).build();
		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(jwtProperties.keyId()).build();
		JwtEncoderParameters params = JwtEncoderParameters.from(header, claims);
		return jwtEncoder.encode(params).getTokenValue();
	}

	@Override
	public AuthenticatedUser verifyAccessToken(String token) {
		if (token == null || token.isBlank()) {
			throw new BadJwtException("Invalid access token");
		}
		Jwt jwt = jwtDecoder.decode(token);
		long userId = Long.parseLong(jwt.getSubject());
		UserRole role = UserRole.valueOf(jwt.getClaimAsString("role"));

		return new AuthenticatedUser(userId, role);
	}
}