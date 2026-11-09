package com.psyweb.auth.ratelimit;

import java.time.Duration;

public record RateLimitDecision(boolean allowed, Duration retryAfter) {

	public RateLimitDecision {
		if (retryAfter == null || retryAfter.isNegative()) {
			throw new IllegalArgumentException("Retry-after cannot be null or negative");
		}

		if (allowed && !retryAfter.isZero()) {
			throw new IllegalArgumentException("Allowed requests cannot have retry-after");
		}

		if (!allowed && retryAfter.isZero()) {
			throw new IllegalArgumentException("Rejected requests must have positive retry-after");
		}
	}
}