package com.psyweb.availability.web.dto.request;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateAvailabilitySlotRequest (
		@NotNull(message = "Start time cannot be null")
		Instant startTime, 
		
		@NotNull(message = "End time cannot be null")
		Instant endTime,
		
		@PositiveOrZero(message = "Minimum booking notice override minutes must be positive or zero")
		Long minimumBookingNoticeOverrideMinutes) {
}
