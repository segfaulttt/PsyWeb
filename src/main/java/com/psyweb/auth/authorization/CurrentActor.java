package com.psyweb.auth.authorization;

import com.psyweb.user.domain.UserRole;

public record CurrentActor (Long userId, UserRole role) {
	public CurrentActor {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("User ID must be positive");
		}
		if (role == null) {
			throw new IllegalArgumentException("User role cannot be null");
		}
	}
}
