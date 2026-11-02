package com.psyweb.auth.exception;

import com.psyweb.common.exception.UnauthorizedException;

public final class UnauthenticatedActorException extends UnauthorizedException{
	private static final String CODE = "UNAUTHENTICATED_ACTOR";

	public UnauthenticatedActorException(String message) {
		super(CODE, message);
	}

	public UnauthenticatedActorException(String message, Throwable cause) {
		super(CODE, message, cause);
	}
}
