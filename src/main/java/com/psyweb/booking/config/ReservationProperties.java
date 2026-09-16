package com.psyweb.booking.config;

import java.time.Duration;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@ConfigurationProperties(prefix = "psyweb.reservation")
@Validated
public record ReservationProperties(
        @NonNull Duration ttl,
        @NotNull Duration expirationScanInterval,
        @Positive int expirationBatchSize
) {
}
