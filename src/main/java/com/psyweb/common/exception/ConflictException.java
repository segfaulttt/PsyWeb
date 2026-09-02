package com.psyweb.common.exception;

public abstract class ConflictException extends BaseDomainException{
	protected ConflictException(String code, String message) {
		super(code, message);
	}
	
	protected ConflictException(String code, String message, Throwable cause) {
		super(code, message, cause);
	}
}
