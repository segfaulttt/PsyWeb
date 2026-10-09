package com.psyweb.specialist.web.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.psyweb.specialist.web.dto.request.UpdateSpecialistBookingSettingsRequest;
import com.psyweb.specialist.web.dto.request.UpdateSpecialistProfileRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class SpecialistRequestValidationTest {

	private Validator validator;

	@BeforeEach
	void setUp() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void shouldAcceptValidProfile() {
		UpdateSpecialistProfileRequest request = new UpdateSpecialistProfileRequest("Anna", "Smith");

		Set<ConstraintViolation<UpdateSpecialistProfileRequest>> violations = validator.validate(request);

		assertTrue(violations.isEmpty());
	}

	@Test
	void shouldRejectBlankFirstName() {
		UpdateSpecialistProfileRequest request = new UpdateSpecialistProfileRequest(" ", "Smith");

		Set<ConstraintViolation<UpdateSpecialistProfileRequest>> violations = validator.validate(request);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldRejectBlankLastName() {
		UpdateSpecialistProfileRequest request = new UpdateSpecialistProfileRequest("Anna", " ");

		Set<ConstraintViolation<UpdateSpecialistProfileRequest>> violations = validator.validate(request);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldAcceptZeroBookingNotices() {
		UpdateSpecialistBookingSettingsRequest request = new UpdateSpecialistBookingSettingsRequest(0L, 0L);

		Set<ConstraintViolation<UpdateSpecialistBookingSettingsRequest>> violations = validator.validate(request);

		assertTrue(violations.isEmpty());
	}

	@Test
	void shouldRejectNegativeMinimumBookingNotice() {
		UpdateSpecialistBookingSettingsRequest request = new UpdateSpecialistBookingSettingsRequest(-1L, 60L);

		Set<ConstraintViolation<UpdateSpecialistBookingSettingsRequest>> violations = validator.validate(request);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldRejectNegativeClientCancellationNotice() {
		UpdateSpecialistBookingSettingsRequest request = new UpdateSpecialistBookingSettingsRequest(60L, -1L);

		Set<ConstraintViolation<UpdateSpecialistBookingSettingsRequest>> violations = validator.validate(request);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldRejectNullMinimumBookingNotice() {
		UpdateSpecialistBookingSettingsRequest request = new UpdateSpecialistBookingSettingsRequest(null, 60L);

		Set<ConstraintViolation<UpdateSpecialistBookingSettingsRequest>> violations = validator.validate(request);

		assertFalse(violations.isEmpty());
	}

	@Test
	void shouldRejectNullClientCancellationNotice() {
		UpdateSpecialistBookingSettingsRequest request = new UpdateSpecialistBookingSettingsRequest(60L, null);

		Set<ConstraintViolation<UpdateSpecialistBookingSettingsRequest>> violations = validator.validate(request);

		assertFalse(violations.isEmpty());
	}
}