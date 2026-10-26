package com.psyweb.auth.service;

import com.psyweb.auth.service.dto.IssuedAccessToken;
import com.psyweb.user.domain.UserRole;

public interface AccessTokenService {

	IssuedAccessToken generateAccessToken(Long userId, UserRole role);

    AuthenticatedUser verifyAccessToken(String token);
}
