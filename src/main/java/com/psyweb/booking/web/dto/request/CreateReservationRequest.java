package com.psyweb.booking.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateReservationRequest (
		@NotNull(message = "Slot ID cannot be null")
		@Positive(message = "Slot ID must be positive")
		Long slotId) {
}
