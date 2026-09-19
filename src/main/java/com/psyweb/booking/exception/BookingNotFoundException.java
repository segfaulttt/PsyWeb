package com.psyweb.booking.exception;

import com.psyweb.common.exception.NotFoundException;

public final class BookingNotFoundException extends NotFoundException {
	private static final String CODE = "BOOKING_NOT_FOUND";
	
	public BookingNotFoundException(String message) {
		super(CODE, message);
	}
	
	public BookingNotFoundException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
