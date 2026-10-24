package com.psyweb.auth.config;

import java.time.Duration;

import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "psyweb.auth.jwt")
@Validated
public record JwtProperties (
		@NotNull
		Path privateKeyPath,
		@NotNull
		Path publicKeyPath,
		@NotBlank
		String keyId,
		@NotBlank
		String issuer,
		@NotBlank
		String audience,
		@NotNull
		@DurationMin(seconds = 1)
		Duration accessTokenTtl){
}
