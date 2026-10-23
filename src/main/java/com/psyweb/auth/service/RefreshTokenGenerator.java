package com.psyweb.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class RefreshTokenGenerator {
	private static final SecureRandom secureRandom = new SecureRandom();
	
	public String generateRawRefreshToken() {
		byte[] randomBytes = new byte[32];
		secureRandom.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}
	
	public String hashToken(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new IllegalArgumentException("Incorrect raw token");
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] rawBytes = rawToken.getBytes(StandardCharsets.UTF_8);
			byte[] hashedBytes = digest.digest(rawBytes);
			return HexFormat.of().formatHex(hashedBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
		}
	}
}
