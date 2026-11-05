package com.psyweb.booking.service.model;

import java.time.Instant;

public record BookingObligation(
        Long id,
        Instant startTime,
        Instant endTime,
        ObligationType type
) {}