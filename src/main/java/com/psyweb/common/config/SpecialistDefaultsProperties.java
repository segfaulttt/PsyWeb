package com.psyweb.common.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "psyweb.specialist.defaults")
public record SpecialistDefaultsProperties(
        Duration minimumBookingNotice,
        Duration clientCancellationNotice
) {}
