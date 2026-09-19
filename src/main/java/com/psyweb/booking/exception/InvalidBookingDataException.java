package com.psyweb.booking.exception;

import com.psyweb.common.exception.ValidationException;

public final class InvalidBookingDataException extends ValidationException {
	private static final String CODE = "BOOKING_INVALID_DATA";
	
	public InvalidBookingDataException(String message) {
		super(CODE, message);
	}
	
	public InvalidBookingDataException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
