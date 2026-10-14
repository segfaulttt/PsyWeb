package com.psyweb.auth.exception;

import com.psyweb.common.exception.ValidationException;

public final class InvalidPasswordException extends ValidationException {
	private static final String CODE = "PASSWORD_INVALID";
	
	public InvalidPasswordException(String message) {
		super(CODE, message);
	}
	
	public InvalidPasswordException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
