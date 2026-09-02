package com.psyweb.common.exception;

public abstract class ForbiddenException extends BaseDomainException{
	protected ForbiddenException(String code, String message) {
		super(code, message);
	}
	
	protected ForbiddenException(String code, String message, Throwable cause) {
		super(code, message, cause);
	}
}
