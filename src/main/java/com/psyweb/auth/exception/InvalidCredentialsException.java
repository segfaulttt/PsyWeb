package com.psyweb.auth.exception;

import com.psyweb.common.exception.UnauthorizedException;

public final class InvalidCredentialsException extends UnauthorizedException {

    private static final String CODE = "INVALID_CREDENTIALS";
    private static final String MESSAGE = "Invalid email or password";

    public InvalidCredentialsException() {
        super(CODE, MESSAGE);
    }
}