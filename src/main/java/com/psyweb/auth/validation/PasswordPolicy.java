package com.psyweb.auth.validation;

import com.psyweb.auth.exception.InvalidPasswordException;

public class PasswordPolicy {
	public static void validatePassword(String rawPassword) {
		if (rawPassword == null) {
			throw new InvalidPasswordException("Password cannot be null");
		}
		int length = rawPassword.codePointCount(0, rawPassword.length());
		if (length < 15) {
			throw new InvalidPasswordException("Password must contain at least 15 characters");
		}
		if (length > 128) {
			throw new InvalidPasswordException("Password must not exceed 128 characters");
		}
	}
}
