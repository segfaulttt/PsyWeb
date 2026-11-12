package com.psyweb.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.psyweb.auth.domain.RefreshSession;
import com.psyweb.auth.exception.InvalidCurrentPasswordException;
import com.psyweb.auth.exception.InvalidRefreshTokenException;
import com.psyweb.auth.repository.RefreshSessionRepository;
import com.psyweb.auth.service.dto.IssuedRefreshToken;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

class PasswordChangeSessionsIntegrationTest extends PostgreSQLIntegrationTest {

	private static final String OLD_PASSWORD = "CurrentPassword123!";
	private static final String NEW_PASSWORD = "NewStrongPassword123!";

	@Autowired
	private PasswordService passwordService;

	@MockitoSpyBean
	private RefreshSessionService refreshSessionService;

	@Autowired
	private RefreshSessionRepository sessionRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private User createUser() {
		return userRepository.saveAndFlush(new User("change-password-" + UUID.randomUUID() + "@example.com",
				passwordEncoder.encode(OLD_PASSWORD), UserRole.CLIENT, UserStatus.ACTIVE));
	}

	@Test
	void shouldRevokeAllRefreshSessionsAfterPasswordChange() {
		User user = createUser();

		IssuedRefreshToken first = refreshSessionService.createSession(user.getId());

		IssuedRefreshToken second = refreshSessionService.createSession(user.getId());

		List<RefreshSession> before = sessionRepository.findByUserId(user.getId());

		assertEquals(2, before.size());
		assertTrue(before.stream().allMatch(session -> session.getRevokedAt() == null));

		passwordService.changePassword(user.getId(), OLD_PASSWORD, NEW_PASSWORD);

		User updated = userRepository.findById(user.getId()).orElseThrow();

		assertTrue(passwordEncoder.matches(NEW_PASSWORD, updated.getPasswordHash()));

		assertFalse(passwordEncoder.matches(OLD_PASSWORD, updated.getPasswordHash()));

		List<RefreshSession> after = sessionRepository.findByUserId(user.getId());

		assertEquals(2, after.size());
		assertTrue(after.stream().allMatch(session -> session.getRevokedAt() != null));

		assertThrows(InvalidRefreshTokenException.class, () -> refreshSessionService.rotation(first.rawRefreshToken()));

		assertThrows(InvalidRefreshTokenException.class,
				() -> refreshSessionService.rotation(second.rawRefreshToken()));
	}

	@Test
	void shouldPreservePasswordAndSessionsWhenCurrentPasswordIsWrong() {
		User user = createUser();

		refreshSessionService.createSession(user.getId());

		String originalHash = user.getPasswordHash();

		assertThrows(InvalidCurrentPasswordException.class,
				() -> passwordService.changePassword(user.getId(), "IncorrectPassword123!", NEW_PASSWORD));

		User persisted = userRepository.findById(user.getId()).orElseThrow();

		assertEquals(originalHash, persisted.getPasswordHash());

		List<RefreshSession> sessions = sessionRepository.findByUserId(user.getId());

		assertEquals(1, sessions.size());
		assertNull(sessions.getFirst().getRevokedAt());
	}

	@Test
	void shouldRollbackPasswordChangeWhenSessionRevocationFails() {
		User user = createUser();

		refreshSessionService.createSession(user.getId());

		String originalHash = user.getPasswordHash();

		doThrow(new IllegalStateException("Simulated revocation failure")).when(refreshSessionService)
				.logoutAll(user.getId());

		try {
			assertThrows(IllegalStateException.class,
					() -> passwordService.changePassword(user.getId(), OLD_PASSWORD, NEW_PASSWORD));

			User persisted = userRepository.findById(user.getId()).orElseThrow();

			assertEquals(originalHash, persisted.getPasswordHash());

			assertTrue(passwordEncoder.matches(OLD_PASSWORD, persisted.getPasswordHash()));

			assertFalse(passwordEncoder.matches(NEW_PASSWORD, persisted.getPasswordHash()));

			List<RefreshSession> sessions = sessionRepository.findByUserId(user.getId());

			assertEquals(1, sessions.size());
			assertNull(sessions.getFirst().getRevokedAt());

		} finally {
			doNothing().when(refreshSessionService).logoutAll(user.getId());
		}
	}
}