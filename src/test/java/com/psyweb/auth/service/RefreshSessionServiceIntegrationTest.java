
package com.psyweb.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.psyweb.auth.domain.RefreshSession;
import com.psyweb.auth.domain.RefreshToken;
import com.psyweb.auth.repository.RefreshSessionRepository;
import com.psyweb.auth.repository.RefreshTokenRepository;
import com.psyweb.auth.service.dto.IssuedRefreshToken;
import com.psyweb.auth.service.dto.RefreshRotationResult;
import com.psyweb.auth.service.dto.ReplayDetected;
import com.psyweb.auth.service.dto.Success;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

class RefreshSessionServiceIntegrationTest extends PostgreSQLIntegrationTest {

	@Autowired
	private RefreshSessionService refreshSessionService;

	@Autowired
	private RefreshSessionRepository sessionRepository;

	@Autowired
	private RefreshTokenRepository tokenRepository;

	@Autowired
	private RefreshTokenGenerator tokenGenerator;

	@Autowired
	private UserRepository userRepository;

	private User createUser() {
		return userRepository.saveAndFlush(new User("refresh-" + UUID.randomUUID() + "@example.com", "password-hash",
				UserRole.CLIENT, UserStatus.ACTIVE));
	}

	private RefreshSession getOnlySession(Long userId) {
		List<RefreshSession> sessions = sessionRepository.findByUserId(userId);

		assertEquals(1, sessions.size());

		return sessions.getFirst();
	}

	@Test
	void shouldCreateSessionAndPersistInitialRefreshToken() {
		User user = createUser();

		Instant before = Instant.now();

		IssuedRefreshToken result = refreshSessionService.createSession(user.getId());

		Instant after = Instant.now();

		assertNotNull(result);
		assertNotNull(result.rawRefreshToken());
		assertEquals(43, result.rawRefreshToken().length());

		RefreshSession session = getOnlySession(user.getId());

		assertNotNull(session.getId());
		assertNull(session.getRevokedAt());
		assertNull(session.getLastUsedAt());

		assertTrue(Duration.between(session.getCreatedAt().plus(Duration.ofDays(7)), session.getExpiresAt()).abs()
				.compareTo(Duration.ofNanos(1_000)) <= 0);

		assertEquals(session.getExpiresAt().truncatedTo(ChronoUnit.MICROS),
				result.expiresAt().truncatedTo(ChronoUnit.MICROS));

		assertFalse(session.getCreatedAt().isBefore(before));
		assertFalse(session.getCreatedAt().isAfter(after));

		List<RefreshToken> tokens = tokenRepository.findBySessionId(session.getId());

		assertEquals(1, tokens.size());

		RefreshToken token = tokens.getFirst();

		assertNotNull(token.getId());
		assertNull(token.getConsumedAt());

		assertEquals(tokenGenerator.hashToken(result.rawRefreshToken()), token.getTokenHash());

		assertNotEquals(result.rawRefreshToken(), token.getTokenHash());

		assertTrue(tokenRepository.findByTokenHash(token.getTokenHash()).isPresent());
	}

	@Test
	void shouldRotateRefreshTokenAndPersistConsumption() {
		User user = createUser();

		IssuedRefreshToken initial = refreshSessionService.createSession(user.getId());

		RefreshSession session = getOnlySession(user.getId());

		String initialHash = tokenGenerator.hashToken(initial.rawRefreshToken());

		RefreshRotationResult rotationResult = refreshSessionService.rotation(initial.rawRefreshToken());

		Success success = assertInstanceOf(Success.class, rotationResult);

		IssuedRefreshToken rotated = success.issuedRefreshToken();

		assertNotNull(rotated);
		assertNotNull(rotated.rawRefreshToken());

		assertNotEquals(initial.rawRefreshToken(), rotated.rawRefreshToken());

		assertTrue(Duration.between(initial.expiresAt(), rotated.expiresAt()).abs()
				.compareTo(Duration.ofNanos(1_000)) <= 0);

		RefreshToken oldToken = tokenRepository.findByTokenHash(initialHash).orElseThrow();

		assertNotNull(oldToken.getConsumedAt());

		String newHash = tokenGenerator.hashToken(rotated.rawRefreshToken());

		RefreshToken newToken = tokenRepository.findByTokenHash(newHash).orElseThrow();

		assertNull(newToken.getConsumedAt());
		assertNotEquals(oldToken.getId(), newToken.getId());

		assertEquals(2, tokenRepository.findBySessionId(session.getId()).size());

		RefreshSession updatedSession = sessionRepository.findById(session.getId()).orElseThrow();

		assertNotNull(updatedSession.getLastUsedAt());
		assertNull(updatedSession.getRevokedAt());
	}

	@Test
	void shouldDetectReplayAndPersistSessionRevocation() {
		User user = createUser();

		IssuedRefreshToken initial = refreshSessionService.createSession(user.getId());

		RefreshSession session = getOnlySession(user.getId());

		Success firstRotation = assertInstanceOf(Success.class,
				refreshSessionService.rotation(initial.rawRefreshToken()));

		String rotatedRawToken = firstRotation.issuedRefreshToken().rawRefreshToken();

		RefreshRotationResult replayResult = refreshSessionService.rotation(initial.rawRefreshToken());

		assertInstanceOf(ReplayDetected.class, replayResult);

		RefreshSession revokedSession = sessionRepository.findById(session.getId()).orElseThrow();

		assertNotNull(revokedSession.getRevokedAt());

		assertEquals(2, tokenRepository.findBySessionId(session.getId()).size());

		assertThrows(IllegalArgumentException.class, () -> refreshSessionService.rotation(rotatedRawToken));
	}

	@Test
	void shouldAllowSequentialRotationsWithoutCreatingMultipleActiveTokens() {
		User user = createUser();

		IssuedRefreshToken initial = refreshSessionService.createSession(user.getId());

		RefreshSession session = getOnlySession(user.getId());

		Success firstRotation = assertInstanceOf(Success.class,
				refreshSessionService.rotation(initial.rawRefreshToken()));

		Success secondRotation = assertInstanceOf(Success.class,
				refreshSessionService.rotation(firstRotation.issuedRefreshToken().rawRefreshToken()));

		assertNotEquals(firstRotation.issuedRefreshToken().rawRefreshToken(),
				secondRotation.issuedRefreshToken().rawRefreshToken());

		List<RefreshToken> tokens = tokenRepository.findBySessionId(session.getId());

		assertEquals(3, tokens.size());

		long activeTokenCount = tokens.stream().filter(token -> token.getConsumedAt() == null).count();

		assertEquals(1, activeTokenCount);

		RefreshSession updatedSession = sessionRepository.findById(session.getId()).orElseThrow();

		assertNull(updatedSession.getRevokedAt());
		assertNotNull(updatedSession.getLastUsedAt());
	}

	@Test
	void shouldRevokeSessionOnLogout() {
		User user = createUser();

		IssuedRefreshToken issued = refreshSessionService.createSession(user.getId());

		RefreshSession session = getOnlySession(user.getId());

		refreshSessionService.logout(issued.rawRefreshToken());

		RefreshSession updatedSession = sessionRepository.findById(session.getId()).orElseThrow();

		assertNotNull(updatedSession.getRevokedAt());

		assertThrows(IllegalArgumentException.class, () -> refreshSessionService.rotation(issued.rawRefreshToken()));
	}

	@Test
	void shouldAllowRepeatedLogout() {
		User user = createUser();

		IssuedRefreshToken issued = refreshSessionService.createSession(user.getId());

		refreshSessionService.logout(issued.rawRefreshToken());

		RefreshSession session = getOnlySession(user.getId());
		Instant firstRevokedAt = session.getRevokedAt();

		assertNotNull(firstRevokedAt);

		assertDoesNotThrow(() -> refreshSessionService.logout(issued.rawRefreshToken()));

		RefreshSession updatedSession = sessionRepository.findById(session.getId()).orElseThrow();

		assertEquals(firstRevokedAt, updatedSession.getRevokedAt());
	}

	@Test
	void shouldIgnoreUnknownTokenOnLogout() {
		String unknownToken = "unknown-refresh-token-" + UUID.randomUUID();

		assertDoesNotThrow(() -> refreshSessionService.logout(unknownToken));
	}

	@Test
	void shouldRevokeAllUserSessionsOnLogoutAll() {
		User user = createUser();

		IssuedRefreshToken first = refreshSessionService.createSession(user.getId());

		IssuedRefreshToken second = refreshSessionService.createSession(user.getId());

		IssuedRefreshToken third = refreshSessionService.createSession(user.getId());

		List<RefreshSession> sessions = sessionRepository.findByUserId(user.getId());

		assertEquals(3, sessions.size());

		refreshSessionService.logoutAll(user.getId());

		List<RefreshSession> revokedSessions = sessionRepository.findByUserId(user.getId());

		assertEquals(3, revokedSessions.size());

		assertTrue(revokedSessions.stream().allMatch(session -> session.getRevokedAt() != null));

		assertAll(
				() -> assertThrows(IllegalArgumentException.class,
						() -> refreshSessionService.rotation(first.rawRefreshToken())),
				() -> assertThrows(IllegalArgumentException.class,
						() -> refreshSessionService.rotation(second.rawRefreshToken())),
				() -> assertThrows(IllegalArgumentException.class,
						() -> refreshSessionService.rotation(third.rawRefreshToken())));
	}

	@Test
	void shouldNotRevokeOtherUsersSessionsOnLogoutAll() {
		User firstUser = createUser();
		User secondUser = createUser();

		IssuedRefreshToken firstToken = refreshSessionService.createSession(firstUser.getId());

		IssuedRefreshToken secondToken = refreshSessionService.createSession(secondUser.getId());

		refreshSessionService.logoutAll(firstUser.getId());

		RefreshSession firstSession = getOnlySession(firstUser.getId());

		RefreshSession secondSession = getOnlySession(secondUser.getId());

		assertNotNull(firstSession.getRevokedAt());
		assertNull(secondSession.getRevokedAt());

		assertThrows(IllegalArgumentException.class,
				() -> refreshSessionService.rotation(firstToken.rawRefreshToken()));

		assertInstanceOf(Success.class, refreshSessionService.rotation(secondToken.rawRefreshToken()));
	}

}