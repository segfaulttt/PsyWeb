package com.psyweb.specialist.exception;

import com.psyweb.common.exception.NotFoundException;

public final class SpecialistNotFoundException extends NotFoundException {
	private static final String CODE = "SPECIALIST_NOT_FOUND";
	
	public SpecialistNotFoundException(String message) {
		super(CODE, message);
	}
	
	public SpecialistNotFoundException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
