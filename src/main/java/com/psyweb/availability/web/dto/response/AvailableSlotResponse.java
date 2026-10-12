package com.psyweb.availability.web.dto.response;

import java.time.Instant;

public record AvailableSlotResponse (
		Long id,
		Instant startTime,
		Instant endTime) {
}
