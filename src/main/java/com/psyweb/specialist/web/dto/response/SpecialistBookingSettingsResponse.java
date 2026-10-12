package com.psyweb.specialist.web.dto.response;

public record SpecialistBookingSettingsResponse(
		Long minimumBookingNoticeMinutes,
		Long clientCancellationNoticeMinutes) {
}