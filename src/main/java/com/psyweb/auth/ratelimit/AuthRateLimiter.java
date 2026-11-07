package com.psyweb.auth.ratelimit;

public interface AuthRateLimiter {

    RateLimitDecision check(String key, RateLimitPolicy policy);
    
}