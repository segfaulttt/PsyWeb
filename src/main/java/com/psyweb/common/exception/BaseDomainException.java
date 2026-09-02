package com.psyweb.common.exception;

public abstract class BaseDomainException extends RuntimeException implements DomainException {
	private final String code;
	
	protected BaseDomainException(String code, String message) {
		super(message);
		this.code = code;
	}
	
	protected BaseDomainException(String code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}
	
	@Override
	public String code() {
		return code;
	}
}