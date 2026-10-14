package com.psyweb.auth.exception;

import com.psyweb.common.exception.ValidationException;

public final class InvalidCurrentPasswordException extends ValidationException {
	private static final String CODE = "INVALID_CURRENT_PASSWORD";

	public InvalidCurrentPasswordException(String message) {
		super(CODE, message);
	}

	public InvalidCurrentPasswordException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
