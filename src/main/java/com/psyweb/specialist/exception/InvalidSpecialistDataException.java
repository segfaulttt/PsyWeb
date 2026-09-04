package com.psyweb.specialist.exception;

import com.psyweb.common.exception.ValidationException;

public final class InvalidSpecialistDataException extends ValidationException {
	private static final String CODE = "SPECIALIST_INVALID_DATA";
	
	public InvalidSpecialistDataException(String message) {
		super(CODE, message);
	}
	
	public InvalidSpecialistDataException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}