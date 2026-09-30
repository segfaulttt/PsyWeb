package com.psyweb.user.exception;

import com.psyweb.common.exception.InvalidStateException;

public class InvalidUserStateException extends InvalidStateException {
private static final String CODE = "USER_INVALID_STATE";
	
	public InvalidUserStateException(String message) {
		super(CODE, message);
	}
	public InvalidUserStateException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
