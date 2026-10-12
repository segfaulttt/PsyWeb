package com.psyweb.booking.web.dto.response;

import java.time.Instant;

import com.psyweb.booking.domain.BookingStatus;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;

public record BookingResponse (
		Long id,
		Long slotId,
		Long clientId,
		Long specialistId,
		BookingStatus status,
		Instant createdAt,
		Instant cancelledAt,
		Instant startTime,
		Instant endTime,
		CancellationInitiator cancellationInitiator,
		CancellationReason cancellationReason) {
}
