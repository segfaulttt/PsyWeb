package com.psyweb.user.exception;

import com.psyweb.common.exception.ConflictException;

public final class ConcurrentPasswordChangeException extends ConflictException {
	private static final String CODE = "PASSWORD_CONFLICT";
	
	public ConcurrentPasswordChangeException(String message) {
		super(CODE, message);
	}
	
	public ConcurrentPasswordChangeException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
