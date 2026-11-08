package com.psyweb.auth.exception;

import com.psyweb.common.exception.ValidationException;

public final class InvalidPasswordResetTokenException extends ValidationException {
    private static final String CODE = "INVALID_PASSWORD_RESET_TOKEN";
    private static final String MESSAGE = "Invalid or expired password reset token";

    public InvalidPasswordResetTokenException() {
        super(CODE, MESSAGE);
    }
}
