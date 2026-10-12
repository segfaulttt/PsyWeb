package com.psyweb.specialist.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateSpecialistBookingSettingsRequest (
		@NotNull(message = "Minimum booking notice minutes cannot be null")
	    @PositiveOrZero(message = "Minimum booking notice minutes must be positive or zero")
		Long minimumBookingNoticeMinutes,
		
		@NotNull(message = "Client cancellation notice minutes cannot be null")
	    @PositiveOrZero(message = "Client cancellation notice minutes must be positive or zero")
		Long clientCancellationNoticeMinutes) {
}