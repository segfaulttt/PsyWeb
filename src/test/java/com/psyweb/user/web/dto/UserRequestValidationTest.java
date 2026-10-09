package com.psyweb.user.web.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.psyweb.user.web.dto.request.ChangeEmailRequest;
import com.psyweb.user.web.dto.request.ChangePasswordRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class UserRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation
                .buildDefaultValidatorFactory()
                .getValidator();
    }
    
    //email:
    
    @Test
    void shouldAcceptValidEmail() {
        ChangeEmailRequest request =
                new ChangeEmailRequest("client@example.com");

        Set<ConstraintViolation<ChangeEmailRequest>> violations =
                validator.validate(request);

        assertTrue(violations.isEmpty());
    }
    
    @Test
    void shouldRejectBlankEmail() {
        ChangeEmailRequest request =
                new ChangeEmailRequest(" ");

        Set<ConstraintViolation<ChangeEmailRequest>> violations =
                validator.validate(request);

        assertFalse(violations.isEmpty());
    }
    
    @Test
    void shouldRejectInvalidEmailFormat() {
        ChangeEmailRequest request =
                new ChangeEmailRequest("not-an-email");

        Set<ConstraintViolation<ChangeEmailRequest>> violations =
                validator.validate(request);

        assertFalse(violations.isEmpty());
    }
    
    // password:
    
    @Test
    void shouldAcceptValidPasswordChangeRequest() {
        ChangePasswordRequest request =
                new ChangePasswordRequest(
                        "current-password",
                        "new-password"
                );

        Set<ConstraintViolation<ChangePasswordRequest>> violations =
                validator.validate(request);

        assertTrue(violations.isEmpty());
    }
    
    @Test
    void shouldRejectBlankCurrentPassword() {
        ChangePasswordRequest request =
                new ChangePasswordRequest(
                        " ",
                        "new-password"
                );

        Set<ConstraintViolation<ChangePasswordRequest>> violations =
                validator.validate(request);

        assertFalse(violations.isEmpty());
    }
    
    @Test
    void shouldRejectBlankNewPassword() {
        ChangePasswordRequest request =
                new ChangePasswordRequest(
                        "current-password",
                        " "
                );

        Set<ConstraintViolation<ChangePasswordRequest>> violations =
                validator.validate(request);

        assertFalse(violations.isEmpty());
    }
}