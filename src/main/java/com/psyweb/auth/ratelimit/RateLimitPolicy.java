package com.psyweb.auth.ratelimit;

import java.time.Duration;

public record RateLimitPolicy(long capacity, Duration refillPeriod) {

	public RateLimitPolicy {
		if (capacity <= 0) {
			throw new IllegalArgumentException("Rate limit capacity must be positive");
		}

		if (refillPeriod == null || refillPeriod.isNegative() || refillPeriod.isZero()) {
			throw new IllegalArgumentException("Refill period must be positive");
		}
	}
}