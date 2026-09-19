package com.psyweb.booking.exception;

import com.psyweb.common.exception.InvalidStateException;

public final class InvalidReservationStateException extends InvalidStateException {
	private static final String CODE = "RESERVATION_INVALID_STATE";
	
	public InvalidReservationStateException(String message) {
		super(CODE, message);
	}
	
	public InvalidReservationStateException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
