package com.psyweb.common.exception;

public abstract class NotFoundException extends BaseDomainException{
	protected NotFoundException(String code, String message) {
		super(code, message);
	}
	
	protected NotFoundException(String code, String message, Throwable cause) {
		super(code, message, cause);
	}
}
