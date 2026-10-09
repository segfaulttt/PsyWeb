package com.psyweb.user.exception;

import com.psyweb.common.exception.ConflictException;

public final class UserEmailAlreadyExistsException extends ConflictException {
    private static final String CODE = "USER_EMAIL_ALREADY_EXISTS";
    
    public UserEmailAlreadyExistsException(String message) {
    	super(CODE, message);
    }
    
    public UserEmailAlreadyExistsException(String message, Throwable cause) {
    	super(CODE, message, cause);
    }
}