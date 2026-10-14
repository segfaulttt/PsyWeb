package com.psyweb.auth.web.dto.request;

import com.psyweb.user.domain.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegistrationRequest (
		@NotBlank
		@Email
		String email,
		
		@NotBlank
		String rawPassword,
		
		@NotNull
		UserRole role
		) {
}
