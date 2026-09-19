package com.psyweb.availability.exception;

import com.psyweb.common.exception.ValidationException;

public class InvalidAvailabilitySlotDataException extends ValidationException {
	private static final String CODE = "AVAILABILITY_SLOT_INVALID_DATA";
	
	public InvalidAvailabilitySlotDataException(String message) {
		super(CODE, message);
	}
	
	public InvalidAvailabilitySlotDataException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
