package com.psyweb.common.exception;

public abstract class ValidationException extends BaseDomainException{
	protected ValidationException(String code, String message) {
		super(code, message);
	}
	
	protected ValidationException(String code, String message, Throwable cause) {
		super(code, message, cause);
	}
}
