package com.psyweb.auth.service.dto;

import java.time.Instant;

public record IssuedAccessToken(String accessToken, Instant expiresAt) {
}
