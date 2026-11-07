package com.psyweb.auth.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.psyweb.auth.ratelimit.RateLimitPolicy;

@Validated
@ConfigurationProperties(prefix = "psyweb.security.rate-limit")
public record AuthRateLimitProperties(long loginCapacity, Duration loginRefillPeriod, long refreshCapacity,
		Duration refreshRefillPeriod) {
	public AuthRateLimitProperties {
		new RateLimitPolicy(loginCapacity, loginRefillPeriod);
		new RateLimitPolicy(refreshCapacity, refreshRefillPeriod);
	}

	public RateLimitPolicy loginPolicy() {
		return new RateLimitPolicy(loginCapacity, loginRefillPeriod);
	}

	public RateLimitPolicy refreshPolicy() {
		return new RateLimitPolicy(refreshCapacity, refreshRefillPeriod);
	}
}