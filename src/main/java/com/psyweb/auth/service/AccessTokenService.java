package com.psyweb.auth.service;

import com.psyweb.user.domain.UserRole;

public interface AccessTokenService {

    String generateAccessToken(Long userId, UserRole role);

    AuthenticatedUser verifyAccessToken(String token);
}
