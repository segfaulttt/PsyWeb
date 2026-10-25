package com.psyweb.auth.service;

import com.psyweb.user.domain.UserRole;

public record AuthenticatedUser(Long userId, UserRole role) {
}