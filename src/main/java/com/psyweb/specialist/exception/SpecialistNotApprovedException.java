package com.psyweb.specialist.exception;

import com.psyweb.common.exception.InvalidStateException;

public final class SpecialistNotApprovedException extends InvalidStateException {
	private static final String CODE = "SPECIALIST_NOT_APPROVED";
	
	public SpecialistNotApprovedException(String message) {
		super(CODE, message);
	}
	
	public SpecialistNotApprovedException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
