package com.psyweb.specialist.exception;

import com.psyweb.common.exception.InvalidStateException;

public final class SpecialistNotEligibleException extends InvalidStateException {
	private static final String CODE = "SPECIALIST_NOT_ELIGIBLE";
	
	public SpecialistNotEligibleException(String message) {
		super(CODE, message);
	}
	
	public SpecialistNotEligibleException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
