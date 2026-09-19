package com.psyweb.booking.exception;

import com.psyweb.common.exception.ExpiredException;

public final class ReservationExpiredException extends ExpiredException{
	private static final String CODE = "RESERVATION_EXPIRED";
	
	public ReservationExpiredException(String message) {
		super(CODE, message);
	}
	
	public ReservationExpiredException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
