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
import com.psyweb.auth.service.RegistrationService;
import com.psyweb.auth.service.dto.AuthenticationResult;
import com.psyweb.auth.web.cookie.RefreshCookieFactory;
import com.psyweb.auth.web.dto.request.ClientRegistrationRequest;
import com.psyweb.auth.web.dto.request.LoginRequest;
import com.psyweb.auth.web.dto.request.SpecialistRegistrationRequest;
import com.psyweb.auth.web.dto.response.AuthenticationResponse;
import com.psyweb.auth.web.dto.response.RegistrationResponse;
import com.psyweb.auth.web.mapper.AuthWebMapper;
import com.psyweb.auth.web.mapper.RegistrationWebMapper;
import com.psyweb.client.domain.Client;
import com.psyweb.specialist.domain.Specialist;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final RegistrationService registrationService;
	private final RegistrationWebMapper registrationWebMapper;
	private final AuthService authService;
	private final AuthWebMapper authWebMapper;
	private final RefreshCookieFactory cookieFactory;
	private final CurrentActorProvider currentActorProvider;

	public AuthController(RegistrationService registrationService, RegistrationWebMapper registrationWebMapper,
			AuthService authService, AuthWebMapper authWebMapper, RefreshCookieFactory cookieFactory,
			CurrentActorProvider currentActorProvider) {
		this.registrationService = registrationService;
		this.registrationWebMapper = registrationWebMapper;
		this.authService = authService;
		this.authWebMapper = authWebMapper;
		this.cookieFactory = cookieFactory;
		this.currentActorProvider = currentActorProvider;
	}

	@PostMapping("/register/client")
	@ResponseStatus(HttpStatus.CREATED)
	public RegistrationResponse registerClient(@Valid @RequestBody ClientRegistrationRequest request) {
		Client client = registrationService.registerClient(request.email(), request.password(), request.firstName(),
				request.lastName());
		return registrationWebMapper.toResponse(client);
	}

	@PostMapping("/register/specialist")
	@ResponseStatus(HttpStatus.CREATED)
	public RegistrationResponse registerSpecialist(@Valid @RequestBody SpecialistRegistrationRequest request) {
		Specialist specialist = registrationService.registerSpecialist(request.email(), request.password(),
				request.firstName(), request.lastName(), request.timeZone());
		return registrationWebMapper.toResponse(specialist);
	}

	@GetMapping("/csrf")
	public ResponseEntity<CsrfToken> csrf(CsrfToken csrfToken) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(csrfToken);
	}

	@PostMapping("/login")
	public ResponseEntity<AuthenticationResponse> loginUser(@Valid @RequestBody LoginRequest request) {
		AuthenticationResult result = authService.login(request.email(), request.password());

		AuthenticationResponse response = authWebMapper.toAuthenticationResponse(result);

		ResponseCookie cookie = cookieFactory.create(result.refreshToken(), result.refreshExpiresAt());

		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
				.cacheControl(CacheControl.noStore()).body(response);
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthenticationResponse> refresh(
			@CookieValue(name = "__Host-refreshToken") String refreshToken) {

		AuthenticationResult result = authService.refresh(refreshToken);

		AuthenticationResponse response = authWebMapper.toAuthenticationResponse(result);

		ResponseCookie cookie = cookieFactory.create(result.refreshToken(), result.refreshExpiresAt());

		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
				.cacheControl(CacheControl.noStore()).body(response);
	}

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

	@PostMapping("/logout-all")
	public ResponseEntity<Void> logoutAll() {
		Long userId = currentActorProvider.requireCurrentActor().userId();

		authService.logoutAll(userId);

		ResponseCookie cookie = cookieFactory.clear();

		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString())
				.cacheControl(CacheControl.noStore()).build();
	}
}
