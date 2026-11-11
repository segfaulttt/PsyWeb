package com.psyweb.user.exception;

import com.psyweb.common.exception.UnauthorizedException;

public final class UserRoleMismatchException extends UnauthorizedException {
    private static final String CODE = "USER_ROLE_MISMATCH";

    public UserRoleMismatchException(String message) {
        super(CODE, message);
    }

    public UserRoleMismatchException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}