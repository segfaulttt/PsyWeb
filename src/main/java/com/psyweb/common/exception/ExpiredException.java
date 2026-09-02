package com.psyweb.common.exception;

public abstract class ExpiredException extends BaseDomainException{
	protected ExpiredException(String code, String message) {
		super(code, message);
	}
	
	protected ExpiredException(String code, String message, Throwable cause) {
		super(code, message, cause);
	}
}
