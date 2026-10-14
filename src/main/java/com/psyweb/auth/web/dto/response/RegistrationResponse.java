package com.psyweb.auth.web.dto.response;

import com.psyweb.user.domain.UserRole;

public record RegistrationResponse (
		Long id,
		UserRole role
		) {
}
