
package com.psyweb.availability.web.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.psyweb.availability.web.dto.request.CreateAvailabilitySlotRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CreateAvailabilitySlotRequestValidationTest {

	private Validator validator;

	private final Instant startTime = Instant.parse("2026-11-15T10:00:00Z");

	private final Instant endTime = Instant.parse("2026-11-15T11:00:00Z");

	@BeforeEach
	void setUp() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void shouldAcceptValidRequest() {
		CreateAvailabilitySlotRequest request = new CreateAvailabilitySlotRequest(startTime, endTime, 60L);

		Set<ConstraintViolation<CreateAvailabilitySlotRequest>> violations = validator.validate(request);

		assertTrue(violations.isEmpty());
	}

	@Test
	void shouldAcceptNullMinimumBookingNoticeOverride() {
		CreateAvailabilitySlotRequest request = new CreateAvailabilitySlotRequest(startTime, endTime, null);

		assertTrue(validator.validate(request).isEmpty());
	}

	@Test
	void shouldAcceptZeroMinimumBookingNoticeOverride() {
		CreateAvailabilitySlotRequest request = new CreateAvailabilitySlotRequest(startTime, endTime, 0L);

		assertTrue(validator.validate(request).isEmpty());
	}

	@Test
	void shouldRejectNegativeMinimumBookingNoticeOverride() {
		CreateAvailabilitySlotRequest request = new CreateAvailabilitySlotRequest(startTime, endTime, -1L);

		assertFalse(validator.validate(request).isEmpty());
	}

	@Test
	void shouldRejectNullStartTime() {
		CreateAvailabilitySlotRequest request = new CreateAvailabilitySlotRequest(null, endTime, 60L);

		assertFalse(validator.validate(request).isEmpty());
	}

	@Test
	void shouldRejectNullEndTime() {
		CreateAvailabilitySlotRequest request = new CreateAvailabilitySlotRequest(startTime, null, 60L);

		assertFalse(validator.validate(request).isEmpty());
	}
}