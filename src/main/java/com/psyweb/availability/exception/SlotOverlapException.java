package com.psyweb.availability.exception;

import com.psyweb.common.exception.ConflictException;

public class SlotOverlapException extends ConflictException {
	private static final String CODE = "SLOT_OVERLAP";
	
	public SlotOverlapException(String message) {
		super(CODE, message);
	}
	
	public SlotOverlapException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
