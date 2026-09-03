package com.psyweb.user.exception;

import com.psyweb.common.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    private static final String CODE = "USER_NOT_FOUND";
    
    public UserNotFoundException(String message) {
    	super(CODE, message);
    }
    
    public UserNotFoundException(String message, Throwable cause) {
    	super(CODE, message, cause);
    }
}
