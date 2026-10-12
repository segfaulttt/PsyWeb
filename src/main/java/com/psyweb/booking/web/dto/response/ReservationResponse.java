package com.psyweb.booking.web.dto.response;

import java.time.Instant;

import com.psyweb.booking.domain.ReservationStatus;

public record ReservationResponse (
		Long id, 
		ReservationStatus status,
		Long slotId,
		Instant expiresAt) {
}
