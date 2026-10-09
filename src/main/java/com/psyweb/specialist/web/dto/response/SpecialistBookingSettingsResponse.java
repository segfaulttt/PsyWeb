package com.psyweb.specialist.web.dto.response;

public record SpecialistBookingSettingsResponse(
		Integer minimumBookingNotice,
		Integer clientCancellationNotice) {
}