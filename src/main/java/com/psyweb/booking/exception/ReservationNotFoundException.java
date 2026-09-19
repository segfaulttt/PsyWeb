package com.psyweb.booking.exception;

import com.psyweb.common.exception.NotFoundException;

public final class ReservationNotFoundException extends NotFoundException {
	private static final String CODE = "RESERVATION_NOT_FOUND";
	
	public ReservationNotFoundException(String message) {
		super(CODE, message);
	}
	
	public ReservationNotFoundException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
