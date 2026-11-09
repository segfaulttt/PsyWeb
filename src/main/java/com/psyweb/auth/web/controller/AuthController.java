package com.psyweb.auth.web.controller;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;

import com.psyweb.auth.authorization.CurrentActorProvider;
import com.psyweb.auth.service.AuthService;
import com.psyweb.auth.service.PasswordResetService;
import com.psyweb.auth.service.RegistrationService;
import com.psyweb.auth.service.dto.AuthenticationResult;
import com.psyweb.auth.web.cookie.RefreshCookieFactory;
import com.psyweb.auth.web.dto.request.ClientRegistrationRequest;
import com.psyweb.auth.web.dto.request.LoginRequest;
import com.psyweb.auth.web.dto.request.PasswordResetConfirmRequest;
import com.psyweb.auth.web.dto.request.PasswordResetRequest;
import com.psyweb.auth.web.dto.request.SpecialistRegistrationRequest;
import com.psyweb.auth.web.dto.response.AuthenticationResponse;
import com.psyweb.auth.web.dto.response.RegistrationResponse;
import com.psyweb.auth.web.mapper.AuthWebMapper;
import com.psyweb.auth.web.mapper.RegistrationWebMapper;
import com.psyweb.client.domain.Client;
import com.psyweb.specialist.domain.Specialist;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

@Tag(name = "Authentication endpoints")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final RegistrationService registrationService;
	private final RegistrationWebMapper registrationWebMapper;
	private final AuthService authService;
	private final AuthWebMapper authWebMapper;
	private final RefreshCookieFactory cookieFactory;
	private final CurrentActorProvider currentActorProvider;
	private final PasswordResetService passwordResetService;

	public AuthController(RegistrationService registrationService, RegistrationWebMapper registrationWebMapper,
			AuthService authService, AuthWebMapper authWebMapper, RefreshCookieFactory cookieFactory,
			CurrentActorProvider currentActorProvider, PasswordResetService passwordResetService) {
		this.registrationService = registrationService;
		this.registrationWebMapper = registrationWebMapper;
		this.authService = authService;
		this.authWebMapper = authWebMapper;
		this.cookieFactory = cookieFactory;
		this.currentActorProvider = currentActorProvider;
		this.passwordResetService = passwordResetService;
	}

	@Operation(
	        summary = "Register client",
	        description = "Creates a new client account."
	)
	@ApiResponse(
	        responseCode = "201",
	        description = "Client registered successfully"
	)
	@PostMapping("/register/client")
	@ResponseStatus(HttpStatus.CREATED)
	public RegistrationResponse registerClient(@Valid @RequestBody ClientRegistrationRequest request) {
		Client client = registrationService.registerClient(request.email(), request.password(), request.firstName(),
				request.lastName());
		return registrationWebMapper.toResponse(client);
	}

	
	@Operation(
	        summary = "Register specialist",
	        description = "Creates a new specialist account."
	)
	@ApiResponse(
	        responseCode = "201",
	        description = "Specialist registered successfully"
	)
	@PostMapping("/register/specialist")
	@ResponseStatus(HttpStatus.CREATED)
	public RegistrationResponse registerSpecialist(@Valid @RequestBody SpecialistRegistrationRequest request) {
		Specialist specialist = registrationService.registerSpecialist(request.email(), request.password(),
				request.firstName(), request.lastName(), request.timeZone());
		return registrationWebMapper.toResponse(specialist);
	}

	
	@Operation(
	        summary = "Get CSRF token",
	        description = "Returns a CSRF token for protecting subsequent state-changing requests."
	)
	@ApiResponse(
	        responseCode = "200",
	        description = "CSRF token retrieved successfully"
	)
	@GetMapping("/csrf")
	public ResponseEntity<CsrfToken> csrf(CsrfToken csrfToken) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(csrfToken);
	}

	
	@Operation(
			summary = "User login",
			description = "Authenticates the user and returns an access token. Sets a refresh token in an HTTP-only cookie."
	)
	@ApiResponse(
			responseCode = "200",
			description = "User logged in successfully",
			headers = @Header(name = "Set-Cookie", description = "Sets the refresh token cookie used to renew authentication.", schema = @Schema(type = "string"))
	)
	@PostMapping("/login")
	public ResponseEntity<AuthenticationResponse> loginUser(@Valid @RequestBody LoginRequest request) {
		AuthenticationResult result = authService.login(request.email(), request.password());

		AuthenticationResponse response = authWebMapper.toAuthenticationResponse(result);

		ResponseCookie cookie = cookieFactory.create(result.refreshToken(), result.refreshExpiresAt());

		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
				.cacheControl(CacheControl.noStore()).body(response);
	}

	
	@Operation(
			summary = "Refresh authentication",
			description = "Issues a new access token using a valid refresh token cookie and rotates the refresh token."
	)
	@ApiResponse(
			responseCode = "200",
			description = "Authentication refreshed successfully",
			headers = @Header(name = "Set-Cookie", description = "Sets the rotated refresh token cookie.", schema = @Schema(type = "string"))
	)
	@PostMapping("/refresh")
	public ResponseEntity<AuthenticationResponse> refresh(
			@CookieValue(name = "__Host-refreshToken") String refreshToken) {

		AuthenticationResult result = authService.refresh(refreshToken);

		AuthenticationResponse response = authWebMapper.toAuthenticationResponse(result);

		ResponseCookie cookie = cookieFactory.create(result.refreshToken(), result.refreshExpiresAt());

		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
				.cacheControl(CacheControl.noStore()).body(response);
	}

	
	@Operation(
			summary = "Log out",
			description = "Revokes the provided refresh token, if present, and clears the refresh token cookie."
	)
	@ApiResponse(
			responseCode = "204",
			description = "Logged out successfully",
			headers = @Header(name = "Set-Cookie", description = "Clears the refresh token cookie.", schema = @Schema(type = "string"))
	)
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
			@CookieValue(name = "__Host-refreshToken", required = false) String refreshToken) {

		if (refreshToken != null && !refreshToken.isBlank()) {
			authService.logout(refreshToken);
		}

		ResponseCookie cookie = cookieFactory.clear();

		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString())
				.cacheControl(CacheControl.noStore()).build();
	}

	
	@Operation(
			summary = "Log out from all devices",
			description = "Revokes all refresh tokens belonging to the authenticated user and clears the current refresh token cookie.",
			security = @SecurityRequirement(name = "Bearer auth")
	)
	@ApiResponse(
			responseCode = "204",
			description = "Logged out from all devices successfully",
			headers = @Header(name = "Set-Cookie", description = "Clears the refresh token cookie on the current device.", schema = @Schema(type = "string"))
	)
	@PostMapping("/logout-all")
	public ResponseEntity<Void> logoutAll() {
		Long userId = currentActorProvider.requireCurrentActor().userId();

		authService.logoutAll(userId);

		ResponseCookie cookie = cookieFactory.clear();

		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString())
				.cacheControl(CacheControl.noStore()).build();
	}

	
	@Operation(
			summary = "Request password reset",
			description = "Accepts a password reset request for the provided email address."
	)
	@ApiResponse(
			responseCode = "202",
			description = "Password reset request accepted"
	)
	@PostMapping("/password-reset/request")
	public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
		passwordResetService.requestPasswordReset(request.email());
		return ResponseEntity.accepted().build();
	}

	
	@Operation(
			summary = "Confirm password reset",
			description = "Resets the account password using a valid password reset token."
	)
	@ApiResponse(
			responseCode = "204",
			description = "Password reset successfully"
	)
	@PostMapping("/password-reset/confirm")
	public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
		passwordResetService.confirmPasswordReset(request.token(), request.newPassword());
		return ResponseEntity.noContent().build();
	}
}
