package com.psyweb.availability.web.dto.response;

import java.time.Instant;

import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;

public record SpecialistAvailabilitySlotResponse (
		Long id,
		Instant startTime,
		Instant endTime,
		AvailabilityStatus status,
		Long minimumBookingNoticeOverrideMinutes,
		Long effectiveMinimumBookingNoticeMinutes,
		Instant cancelledAt,
		CancellationInitiator cancellationInitiator, 
		CancellationReason cancellationReason) {
}
