package com.psyweb.common.exception;

public abstract class InvalidStateException extends BaseDomainException{
	protected InvalidStateException(String code, String message) {
		super(code, message);
	}
	
	protected InvalidStateException(String code, String message, Throwable cause) {
		super(code, message, cause);
	}
}
