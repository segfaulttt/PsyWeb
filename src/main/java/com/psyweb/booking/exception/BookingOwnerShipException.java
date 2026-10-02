package com.psyweb.booking.exception;

import com.psyweb.common.exception.ForbiddenException;

public final class BookingOwnerShipException extends ForbiddenException {
	private static final String CODE = "BOOKING_OWNERSHIP_VIOLATION";
	
	public BookingOwnerShipException(String message) {
		super(CODE, message);
	}
	
	public BookingOwnerShipException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
