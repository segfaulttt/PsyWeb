
package com.psyweb.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.psyweb.auth.service.dto.AuthenticationResult;
import com.psyweb.auth.service.dto.IssuedAccessToken;
import com.psyweb.auth.service.dto.IssuedRefreshToken;
import com.psyweb.auth.service.dto.ReplayDetected;
import com.psyweb.auth.service.dto.Success;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.exception.UserAccessForbiddenException;
import com.psyweb.user.exception.UserNotFoundException;
import com.psyweb.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final Long USER_ID = 42L;
	private static final String EMAIL = "client@example.com";
	private static final String PASSWORD = "correct-password";
	private static final String PASSWORD_HASH = "stored-password-hash";
	private static final String ACCESS_TOKEN = "signed-access-token";
	private static final String REFRESH_TOKEN = "raw-refresh-token";
	private static final String ROTATED_TOKEN = "rotated-refresh-token";

	private static final Instant ACCESS_EXPIRES_AT = Instant.parse("2026-10-26T12:15:00Z");
	private static final Instant REFRESH_EXPIRES_AT = Instant.parse("2026-11-02T12:00:00Z");

	@Mock
	private UserService userService;

	@Mock
	private AccessTokenService accessTokenService;

	@Mock
	private RefreshSessionService refreshSessionService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private User user;

	@InjectMocks
	private AuthService authService;

	@Test
	void shouldLoginSuccessfully() {
		when(userService.findUserForUpdateByEmail(EMAIL)).thenReturn(user);
		when(user.getPasswordHash()).thenReturn(PASSWORD_HASH);
		when(user.getId()).thenReturn(USER_ID);
		when(user.getRole()).thenReturn(UserRole.CLIENT);
		when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

		when(refreshSessionService.createSession(USER_ID))
				.thenReturn(new IssuedRefreshToken(REFRESH_TOKEN, REFRESH_EXPIRES_AT));

		when(accessTokenService.generateAccessToken(USER_ID, UserRole.CLIENT))
				.thenReturn(new IssuedAccessToken(ACCESS_TOKEN, ACCESS_EXPIRES_AT));

		AuthenticationResult result = authService.login(EMAIL, PASSWORD);

		assertNotNull(result);
		assertEquals(ACCESS_TOKEN, result.accessToken());
		assertEquals(REFRESH_TOKEN, result.refreshToken());
		assertEquals(ACCESS_EXPIRES_AT, result.expiresAt());

		verify(userService).findUserForUpdateByEmail(EMAIL);
		verify(passwordEncoder).matches(PASSWORD, PASSWORD_HASH);
		verify(refreshSessionService).createSession(USER_ID);
		verify(accessTokenService).generateAccessToken(USER_ID, UserRole.CLIENT);
	}

	@Test
	void shouldRejectLoginWithInvalidPassword() {
		when(userService.findUserForUpdateByEmail(EMAIL)).thenReturn(user);
		when(user.getPasswordHash()).thenReturn(PASSWORD_HASH);
		when(passwordEncoder.matches("wrong-password", PASSWORD_HASH)).thenReturn(false);

		assertThrows(IllegalArgumentException.class, () -> authService.login(EMAIL, "wrong-password"));

		verifyNoInteractions(refreshSessionService, accessTokenService);
	}

	@Test
	void shouldRejectLoginWhenUserDoesNotExist() {
		when(userService.findUserForUpdateByEmail(EMAIL)).thenThrow(new UserNotFoundException("User not found"));

		assertThrows(UserNotFoundException.class, () -> authService.login(EMAIL, PASSWORD));

		verifyNoInteractions(passwordEncoder, refreshSessionService, accessTokenService);
	}

	@Test
	void shouldRejectLoginWhenUserIsBlocked() {
		when(userService.findUserForUpdateByEmail(EMAIL)).thenReturn(user);
		when(user.getPasswordHash()).thenReturn(PASSWORD_HASH);
		when(user.getId()).thenReturn(USER_ID);
		when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

		when(refreshSessionService.createSession(USER_ID))
				.thenThrow(new UserAccessForbiddenException("User must have status 'ACTIVE'"));

		assertThrows(UserAccessForbiddenException.class, () -> authService.login(EMAIL, PASSWORD));

		verify(refreshSessionService).createSession(USER_ID);
		verifyNoInteractions(accessTokenService);
	}

	@Test
	void shouldRejectLoginWithBlankEmail() {
		assertThrows(IllegalArgumentException.class, () -> authService.login("  ", PASSWORD));

		verifyNoInteractions(userService, passwordEncoder, refreshSessionService, accessTokenService);
	}

	@Test
	void shouldRejectLoginWithBlankPassword() {
		assertThrows(IllegalArgumentException.class, () -> authService.login(EMAIL, "  "));

		verifyNoInteractions(userService, passwordEncoder, refreshSessionService, accessTokenService);
	}

	@Test
	void shouldPropagateAccessTokenGenerationFailure() {
		when(userService.findUserForUpdateByEmail(EMAIL)).thenReturn(user);
		when(user.getPasswordHash()).thenReturn(PASSWORD_HASH);
		when(user.getId()).thenReturn(USER_ID);
		when(user.getRole()).thenReturn(UserRole.CLIENT);
		when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

		when(refreshSessionService.createSession(USER_ID))
				.thenReturn(new IssuedRefreshToken(REFRESH_TOKEN, REFRESH_EXPIRES_AT));

		when(accessTokenService.generateAccessToken(USER_ID, UserRole.CLIENT))
				.thenThrow(new IllegalStateException("JWT signing failed"));

		assertThrows(IllegalStateException.class, () -> authService.login(EMAIL, PASSWORD));

		verify(refreshSessionService).createSession(USER_ID);
		verify(accessTokenService).generateAccessToken(USER_ID, UserRole.CLIENT);
	}

	@Test
	void shouldRefreshSuccessfully() {
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(USER_ID, UserRole.CLIENT);

		Success success = new Success(new IssuedRefreshToken(ROTATED_TOKEN, REFRESH_EXPIRES_AT), authenticatedUser);

		when(refreshSessionService.rotation(REFRESH_TOKEN)).thenReturn(success);

		when(accessTokenService.generateAccessToken(USER_ID, UserRole.CLIENT))
				.thenReturn(new IssuedAccessToken(ACCESS_TOKEN, ACCESS_EXPIRES_AT));

		AuthenticationResult result = authService.refresh(REFRESH_TOKEN);

		assertEquals(ACCESS_TOKEN, result.accessToken());
		assertEquals(ROTATED_TOKEN, result.refreshToken());
		assertEquals(ACCESS_EXPIRES_AT, result.expiresAt());

		verify(refreshSessionService).rotation(REFRESH_TOKEN);
		verify(accessTokenService).generateAccessToken(USER_ID, UserRole.CLIENT);
		verifyNoInteractions(userService, passwordEncoder);
	}

	@Test
	void shouldNotIssueAccessTokenWhenReplayIsDetected() {
		when(refreshSessionService.rotation(REFRESH_TOKEN)).thenReturn(new ReplayDetected());

		assertThrows(IllegalArgumentException.class, () -> authService.refresh(REFRESH_TOKEN));

		verify(refreshSessionService).rotation(REFRESH_TOKEN);
		verifyNoInteractions(accessTokenService, userService, passwordEncoder);
	}

	@Test
	void shouldLogoutCurrentSession() {
		authService.logout(REFRESH_TOKEN);

		verify(refreshSessionService).logout(REFRESH_TOKEN);
		verifyNoInteractions(userService, accessTokenService, passwordEncoder);
	}

	@Test
	void shouldLogoutAllSessions() {
		authService.logoutAll(USER_ID);

		verify(refreshSessionService).logoutAll(USER_ID);
		verifyNoInteractions(userService, accessTokenService, passwordEncoder);
	}
}
