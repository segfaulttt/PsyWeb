package com.psyweb.common.exception;

public abstract class UnauthorizedException extends BaseDomainException{
	protected UnauthorizedException(String code, String message) {
		super(code, message);
	}
	
	protected UnauthorizedException(String code, String message, Throwable cause) {
		super(code, message, cause);
	}
}
