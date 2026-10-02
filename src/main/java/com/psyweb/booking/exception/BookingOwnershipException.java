package com.psyweb.booking.exception;

import com.psyweb.common.exception.ForbiddenException;

public final class BookingOwnershipException extends ForbiddenException {
	private static final String CODE = "BOOKING_OWNERSHIP_VIOLATION";
	
	public BookingOwnershipException(String message) {
		super(CODE, message);
	}
	
	public BookingOwnershipException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
