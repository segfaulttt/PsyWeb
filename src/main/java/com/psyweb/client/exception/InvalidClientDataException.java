package com.psyweb.client.exception;

import com.psyweb.common.exception.ValidationException;

public class InvalidClientDataException extends ValidationException {
	private static final String CODE = "CLIENT_INVALID_DATA";
	
	public InvalidClientDataException(String message) {
		super(CODE, message);
	}
	
	public InvalidClientDataException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
