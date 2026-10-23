package com.psyweb.auth.config;

import java.time.Duration;

import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@ConfigurationProperties(prefix = "psyweb.auth")
@Validated
public record RefreshSessionProperties(
		@NotNull @DurationMin(seconds = 1) Duration expirationScanInterval, 
		@Positive int expirationBatchSize) {
}
