package com.psyweb.auth.ratelimit;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

@Component
public class InMemoryAuthRateLimiter implements AuthRateLimiter {

	private final Cache<String, Bucket> buckets;

	public InMemoryAuthRateLimiter() {
		this.buckets = Caffeine.newBuilder().maximumSize(100_000).expireAfterAccess(Duration.ofMinutes(5)).build();
	}

	@Override
	public RateLimitDecision check(String key, RateLimitPolicy policy) {
		if (key == null || key.isBlank()) {
			throw new IllegalArgumentException("Rate limit key must not be blank");
		}

		if (policy == null) {
			throw new IllegalArgumentException("Rate limit policy must not be null");
		}

		Bucket bucket = buckets.get(key, k -> createBucket(policy));

		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

		if (probe.isConsumed()) {
			return new RateLimitDecision(true, Duration.ZERO);
		}

		long nanos = probe.getNanosToWaitForRefill();

		return new RateLimitDecision(false, Duration.ofNanos(Math.max(1, nanos)));
	}

	private Bucket createBucket(RateLimitPolicy policy) {
		return Bucket.builder().addLimit(
				limit -> limit.capacity(policy.capacity()).refillGreedy(policy.capacity(), policy.refillPeriod()))
				.build();
	}
}