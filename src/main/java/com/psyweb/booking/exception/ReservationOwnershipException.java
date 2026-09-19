package com.psyweb.booking.exception;

import com.psyweb.common.exception.ForbiddenException;

public final class ReservationOwnershipException extends ForbiddenException {
	private static final String CODE = "RESERVATION_OWNERSHIP_VIOLATION";
	
	public ReservationOwnershipException(String message) {
		super(CODE, message);
	}
	
	public ReservationOwnershipException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
