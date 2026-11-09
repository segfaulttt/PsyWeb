package com.psyweb.auth.exception;

import com.psyweb.common.exception.UnauthorizedException;

public final class InvalidRefreshTokenException extends UnauthorizedException {

    private static final String CODE = "INVALID_REFRESH_TOKEN";
    private static final String MESSAGE = "Invalid or expired refresh token";

    public InvalidRefreshTokenException() {
        super(CODE, MESSAGE);
    }
}