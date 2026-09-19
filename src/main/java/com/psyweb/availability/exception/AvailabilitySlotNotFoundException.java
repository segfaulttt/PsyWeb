package com.psyweb.availability.exception;

import com.psyweb.common.exception.NotFoundException;

public class AvailabilitySlotNotFoundException extends NotFoundException {
	private static final String CODE = "AVAILABILITY_SLOT_NOT_FOUND";
	
	public AvailabilitySlotNotFoundException(String message) {
		super(CODE, message);
	}
	
	public AvailabilitySlotNotFoundException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
