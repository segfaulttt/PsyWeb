package com.psyweb.booking.exception;

import com.psyweb.common.exception.ValidationException;

public final class InvalidReservationDataException extends ValidationException {
	private static final String CODE = "RESERVATION_INVALID_DATA";
	
	public InvalidReservationDataException(String message) {
		super(CODE, message);
	}
	
	public InvalidReservationDataException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
