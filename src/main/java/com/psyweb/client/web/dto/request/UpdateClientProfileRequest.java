package com.psyweb.client.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateClientProfileRequest(
		@NotBlank(message = "First name cannot be blank") 
		String firstName,

		@NotBlank(message = "Last name cannot be blank") 
		String lastName) {
}