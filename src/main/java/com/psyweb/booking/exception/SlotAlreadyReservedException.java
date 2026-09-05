package com.psyweb.booking.exception;

import com.psyweb.common.exception.ConflictException;

public final class SlotAlreadyReservedException extends ConflictException {
	private static final String CODE = "SLOT_ALREADY_RESERVED";
	
	public SlotAlreadyReservedException(String message) {
		super(CODE, message);
	}
	
	public SlotAlreadyReservedException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}