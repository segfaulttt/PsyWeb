package com.psyweb.availability.exception;

import com.psyweb.common.exception.InvalidStateException;

public final class InvalidAvailabilitySlotStateException extends InvalidStateException {

	private static final String CODE = "AVAILABILITY_SLOT_INVALID_STATE";
	
	public InvalidAvailabilitySlotStateException(String message) {
		super(CODE, message);
	}
	
	public InvalidAvailabilitySlotStateException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
