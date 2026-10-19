package com.psyweb.auth.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRegistrationRequest (
		@NotBlank
		@Email
		String email,
		
		@NotBlank
		String password,
		
		@NotBlank
		@Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters long")
		String firstName,
		
		@NotBlank
		@Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters long")
		String lastName) {
}
