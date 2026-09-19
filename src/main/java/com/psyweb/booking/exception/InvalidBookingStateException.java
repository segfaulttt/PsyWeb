package com.psyweb.booking.exception;

import com.psyweb.common.exception.InvalidStateException;

public final class InvalidBookingStateException extends InvalidStateException {
	private static final String CODE = "BOOKING_INVALID_STATE";
	
	public InvalidBookingStateException(String message) {
		super(CODE, message);
	}
	
	public InvalidBookingStateException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
