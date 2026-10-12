package com.psyweb.user.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangeEmailRequest(
		@NotBlank(message = "Email cannot be blank") 
		@Email(message = "Invalid email format") 
		String email) {
}