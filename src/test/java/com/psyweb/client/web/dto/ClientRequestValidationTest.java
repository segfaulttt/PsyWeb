package com.psyweb.client.web.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.psyweb.client.web.dto.request.UpdateClientProfileRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ClientRequestValidationTest {

	private Validator validator;

	@BeforeEach
	void setUp() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void shouldAcceptValidProfile() {
		UpdateClientProfileRequest request = new UpdateClientProfileRequest("Anna", "Smith");

		Set<ConstraintViolation<UpdateClientProfileRequest>> violations = validator.validate(request);

		assertTrue(violations.isEmpty());
	}

	@Test
	void shouldRejectBlankFirstName() {
		UpdateClientProfileRequest request = new UpdateClientProfileRequest(" ", "Smith");

		Set<ConstraintViolation<UpdateClientProfileRequest>> violations = validator.validate(request);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldRejectBlankLastName() {
		UpdateClientProfileRequest request = new UpdateClientProfileRequest("Anna", " ");

		Set<ConstraintViolation<UpdateClientProfileRequest>> violations = validator.validate(request);

		assertFalse(violations.isEmpty());
	}
}