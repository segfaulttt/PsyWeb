package com.psyweb.specialist.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateSpecialistBookingSettingsRequest (
		@NotNull(message = "Minimum booking notice cannot be null")
	    @Positive(message = "Minimum booking notice must be positive")
		Integer minimumBookingNotice,
		
		@NotNull(message = "Client cancellation notice cannot be null")
	    @Positive(message = "Client cancellation notice must be positive")
		Integer clientCancellationNotice) {
}