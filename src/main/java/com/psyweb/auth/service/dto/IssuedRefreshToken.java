package com.psyweb.auth.service.dto;

import java.time.Instant;

public record IssuedRefreshToken(String rawRefreshToken, Instant expiresAt) {
}
