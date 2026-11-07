package com.psyweb.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenGenerator {

	private static final int TOKEN_BYTES = 32;

	private final SecureRandom secureRandom = new SecureRandom();

	public String generateRawToken() {
		byte[] randomBytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(randomBytes);

		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	public String hashToken(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new IllegalArgumentException("Invalid reset token");
		}

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
		}
	}
}