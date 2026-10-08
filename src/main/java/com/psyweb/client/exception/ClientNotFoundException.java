package com.psyweb.client.exception;

import com.psyweb.common.exception.NotFoundException;

public class ClientNotFoundException extends NotFoundException {
	private static final String CODE = "CLIENT_NOT_FOUND";
	
	public ClientNotFoundException(String message) {
		super(CODE, message);
	}
	
	public ClientNotFoundException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
