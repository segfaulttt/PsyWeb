package com.psyweb.auth.web.dto.response;

public record AuthenticationResponse(
		String accessToken, 
		String tokenType, 
		long expiresIn) {
}
