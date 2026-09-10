package com.psyweb.specialist.exception;

import com.psyweb.common.exception.InvalidStateException;

public class InvalidSpecialistStateException extends InvalidStateException {
	private static final String CODE = "SPECIALIST_INVALID_STATE";
	
	public InvalidSpecialistStateException(String message) {
		super(CODE, message);
	}
	
	public InvalidSpecialistStateException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
