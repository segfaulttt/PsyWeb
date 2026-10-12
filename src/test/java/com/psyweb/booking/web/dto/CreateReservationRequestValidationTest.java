
package com.psyweb.booking.web.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.psyweb.booking.web.dto.request.CreateReservationRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CreateReservationRequestValidationTest {

	private Validator validator;

	@BeforeEach
	void setUp() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void shouldAcceptValidSlotId() {
		CreateReservationRequest request = new CreateReservationRequest(1L);

		Set<ConstraintViolation<CreateReservationRequest>> violations = validator.validate(request);

		assertTrue(violations.isEmpty());
	}

	@Test
	void shouldAcceptLargePositiveSlotId() {
		CreateReservationRequest request = new CreateReservationRequest(Long.MAX_VALUE);

		assertTrue(validator.validate(request).isEmpty());
	}

	@Test
	void shouldRejectNullSlotId() {
		CreateReservationRequest request = new CreateReservationRequest(null);

		assertFalse(validator.validate(request).isEmpty());
	}

	@Test
	void shouldRejectZeroSlotId() {
		CreateReservationRequest request = new CreateReservationRequest(0L);

		assertFalse(validator.validate(request).isEmpty());
	}

	@Test
	void shouldRejectNegativeSlotId() {
		CreateReservationRequest request = new CreateReservationRequest(-1L);

		assertFalse(validator.validate(request).isEmpty());
	}
}
