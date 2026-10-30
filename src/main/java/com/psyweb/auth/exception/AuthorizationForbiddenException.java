package com.psyweb.auth.exception;

import com.psyweb.common.exception.ForbiddenException;

public final class AuthorizationForbiddenException extends ForbiddenException {
	private static final String CODE = "AUTHORIZATION_FORBIDDEN";

	public AuthorizationForbiddenException(String message) {
		super(CODE, message);
	}

	public AuthorizationForbiddenException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
