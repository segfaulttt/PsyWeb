package com.psyweb.user.exception;

import com.psyweb.common.exception.ValidationException;

public class InvalidUserDataException extends ValidationException {
	private static final String CODE = "USER_INVALID_DATA";
	
	public InvalidUserDataException(String message) {
		super(CODE, message);
	}
	public InvalidUserDataException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}