package com.psyweb.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.psyweb.auth.exception.InvalidCurrentPasswordException;
import com.psyweb.auth.exception.InvalidPasswordException;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

	private static final Long USER_ID = 1L;
	private static final String CURRENT_PASSWORD = "CurrentPassword123!";
	private static final String NEW_PASSWORD = "NewStrongPassword123!";
	private static final String OLD_HASH = "old-argon2-hash";
	private static final String NEW_HASH = "new-argon2-hash";

	@Mock
	private UserService userService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private RefreshSessionService refreshSessionService;

	@InjectMocks
	private PasswordService passwordService;

	private User activeUser() {
		return new User("client@example.com", OLD_HASH, UserRole.CLIENT, UserStatus.ACTIVE);
	}

	@Test
	void shouldChangePasswordAndRevokeAllRefreshSessions() {
		when(userService.getActiveUserForUpdate(USER_ID)).thenReturn(activeUser());
		when(passwordEncoder.matches(CURRENT_PASSWORD, OLD_HASH)).thenReturn(true);
		when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_HASH);

		passwordService.changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD);

		InOrder order = inOrder(userService, passwordEncoder, refreshSessionService);

		order.verify(userService).getActiveUserForUpdate(USER_ID);
		order.verify(passwordEncoder).matches(CURRENT_PASSWORD, OLD_HASH);
		order.verify(passwordEncoder).encode(NEW_PASSWORD);
		order.verify(userService).changePassword(USER_ID, OLD_HASH, NEW_HASH);
		order.verify(refreshSessionService).logoutAll(USER_ID);

		verifyNoMoreInteractions(refreshSessionService);
	}

	@Test
	void shouldRejectNullUserIdWithoutRevokingSessions() {
		assertThrows(InvalidUserDataException.class,
				() -> passwordService.changePassword(null, CURRENT_PASSWORD, NEW_PASSWORD));

		verifyNoInteractions(userService, passwordEncoder, refreshSessionService);
	}

	@Test
	void shouldRejectNullCurrentPasswordWithoutRevokingSessions() {
		assertThrows(InvalidCurrentPasswordException.class,
				() -> passwordService.changePassword(USER_ID, null, NEW_PASSWORD));

		verifyNoInteractions(userService, passwordEncoder, refreshSessionService);
	}

	@Test
	void shouldRejectIncorrectCurrentPasswordWithoutUpdatingCredentials() {
		when(userService.getActiveUserForUpdate(USER_ID)).thenReturn(activeUser());
		when(passwordEncoder.matches(CURRENT_PASSWORD, OLD_HASH)).thenReturn(false);

		assertThrows(InvalidCurrentPasswordException.class,
				() -> passwordService.changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD));

		verify(passwordEncoder, never()).encode(anyString());
		verify(userService, never()).changePassword(any(), anyString(), anyString());
		verifyNoInteractions(refreshSessionService);
	}

	@Test
	void shouldRejectTooShortNewPasswordWithoutRevokingSessions() {
		when(userService.getActiveUserForUpdate(USER_ID)).thenReturn(activeUser());
		when(passwordEncoder.matches(CURRENT_PASSWORD, OLD_HASH)).thenReturn(true);

		assertThrows(InvalidPasswordException.class,
				() -> passwordService.changePassword(USER_ID, CURRENT_PASSWORD, "short"));

		verify(passwordEncoder, never()).encode(anyString());
		verify(userService, never()).changePassword(any(), anyString(), anyString());
		verifyNoInteractions(refreshSessionService);
	}

	@Test
	void shouldRejectSamePasswordWithoutRevokingSessions() {
		when(userService.getActiveUserForUpdate(USER_ID)).thenReturn(activeUser());
		when(passwordEncoder.matches(CURRENT_PASSWORD, OLD_HASH)).thenReturn(true);

		assertThrows(InvalidPasswordException.class,
				() -> passwordService.changePassword(USER_ID, CURRENT_PASSWORD, CURRENT_PASSWORD));

		verify(passwordEncoder, never()).encode(anyString());
		verify(userService, never()).changePassword(any(), anyString(), anyString());
		verifyNoInteractions(refreshSessionService);
	}

	@Test
	void shouldRejectNullNewPasswordWithoutRevokingSessions() {
		when(userService.getActiveUserForUpdate(USER_ID)).thenReturn(activeUser());
		when(passwordEncoder.matches(CURRENT_PASSWORD, OLD_HASH)).thenReturn(true);

		assertThrows(InvalidPasswordException.class,
				() -> passwordService.changePassword(USER_ID, CURRENT_PASSWORD, null));

		verify(passwordEncoder, never()).encode(anyString());
		verify(userService, never()).changePassword(any(), anyString(), anyString());
		verifyNoInteractions(refreshSessionService);
	}

	@Test
	void shouldAcceptLegacyCurrentPassword() {
		String legacyPassword = "old-pass";

		when(userService.getActiveUserForUpdate(USER_ID)).thenReturn(activeUser());
		when(passwordEncoder.matches(legacyPassword, OLD_HASH)).thenReturn(true);
		when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_HASH);

		assertDoesNotThrow(() -> passwordService.changePassword(USER_ID, legacyPassword, NEW_PASSWORD));

		verify(userService).changePassword(USER_ID, OLD_HASH, NEW_HASH);
		verify(refreshSessionService).logoutAll(USER_ID);
	}

	@Test
	void shouldNotRevokeSessionsIfPasswordUpdateFails() {
		when(userService.getActiveUserForUpdate(USER_ID)).thenReturn(activeUser());
		when(passwordEncoder.matches(CURRENT_PASSWORD, OLD_HASH)).thenReturn(true);
		when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_HASH);

		doThrow(new IllegalStateException("Database update failed")).when(userService).changePassword(USER_ID, OLD_HASH,
				NEW_HASH);

		assertThrows(IllegalStateException.class,
				() -> passwordService.changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD));

		verifyNoInteractions(refreshSessionService);
	}

	@Test
	void shouldPropagateRefreshSessionRevocationFailure() {
		when(userService.getActiveUserForUpdate(USER_ID)).thenReturn(activeUser());
		when(passwordEncoder.matches(CURRENT_PASSWORD, OLD_HASH)).thenReturn(true);
		when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_HASH);

		doThrow(new IllegalStateException("Revocation failed")).when(refreshSessionService).logoutAll(USER_ID);

		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> passwordService.changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD));

		assertEquals("Revocation failed", exception.getMessage());

		verify(userService).changePassword(USER_ID, OLD_HASH, NEW_HASH);
		verify(refreshSessionService).logoutAll(USER_ID);
	}
}