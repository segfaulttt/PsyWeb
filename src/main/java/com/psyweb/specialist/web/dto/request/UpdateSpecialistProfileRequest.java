package com.psyweb.specialist.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateSpecialistProfileRequest (
		@NotBlank(message = "First name cannot be blank")
		String firstName,
		
		@NotBlank(message = "Last name cannot be blank")
		String lastName) {
}