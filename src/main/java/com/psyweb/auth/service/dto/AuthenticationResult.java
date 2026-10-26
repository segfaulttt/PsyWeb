package com.psyweb.auth.service.dto;

import java.time.Instant;

public record AuthenticationResult (String accessToken, String refreshToken, Instant expiresAt){
}
