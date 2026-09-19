package com.psyweb.booking.exception;

import com.psyweb.common.exception.ConflictException;

public final class ActiveReservationAlreadyExistsException extends ConflictException {
	private static final String CODE = "ACTIVE_RESERVATION_ALREADY_EXISTS";
	
	public ActiveReservationAlreadyExistsException(String message) {
		super(CODE, message);
	}
	
	public ActiveReservationAlreadyExistsException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}