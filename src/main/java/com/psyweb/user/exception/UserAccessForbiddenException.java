package com.psyweb.user.exception;

import com.psyweb.common.exception.ForbiddenException;

public final class UserAccessForbiddenException extends ForbiddenException {
	private static final String CODE = "USER_ACCESS_FORBIDDEN";
	
	public UserAccessForbiddenException(String message) {
		super(CODE, message);
	}
	
	public UserAccessForbiddenException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
