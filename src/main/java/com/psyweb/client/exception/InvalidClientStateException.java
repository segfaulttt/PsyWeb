package com.psyweb.client.exception;

import com.psyweb.common.exception.InvalidStateException;

public class InvalidClientStateException extends InvalidStateException {

	private static final String CODE = "CLIENT_INVALID_STATE";
	
	public InvalidClientStateException(String message) {
		super(CODE, message);
	}
	
	public InvalidClientStateException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
