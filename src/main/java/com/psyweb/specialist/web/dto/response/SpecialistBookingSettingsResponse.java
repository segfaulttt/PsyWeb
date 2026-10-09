package com.psyweb.specialist.web.dto.response;

public record SpecialistBookingSettingsResponse(
		long minimumBookingNotice,
		long clientCancellationNotice) {
}