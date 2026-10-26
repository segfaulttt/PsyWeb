package com.psyweb.auth.service.dto;

import com.psyweb.auth.service.AuthenticatedUser;

public record Success(IssuedRefreshToken issuedRefreshToken, AuthenticatedUser authenticatedUser)
		implements RefreshRotationResult {
}
