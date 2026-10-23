
package com.psyweb.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.psyweb.auth.domain.RefreshSession;
import com.psyweb.auth.domain.RefreshToken;
import com.psyweb.auth.repository.RefreshSessionRepository;
import com.psyweb.auth.repository.RefreshTokenRepository;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

@TestPropertySource(properties = { "psyweb.auth.expiration-batch-size=2" })
class RefreshSessionCleanupIntegrationTest extends PostgreSQLIntegrationTest {

	@Autowired
	private RefreshSessionCleanupService cleanupService;

	@Autowired
	private RefreshSessionRepository sessionRepository;

	@Autowired
	private RefreshTokenRepository tokenRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private Clock clock;

	private User createUser() {
		return userRepository.saveAndFlush(new User("cleanup-" + UUID.randomUUID() + "@example.com", "password-hash",
				UserRole.CLIENT, UserStatus.ACTIVE));
	}

	private RefreshSession createSession(User user, Instant expiresAt) {
		Instant createdAt = expiresAt.minus(Duration.ofDays(7));

		return sessionRepository.saveAndFlush(new RefreshSession(user, createdAt, expiresAt));
	}

	private RefreshToken createToken(RefreshSession session) {
		return tokenRepository.saveAndFlush(new RefreshToken(session,
				UUID.randomUUID().toString().replace("-", "").repeat(2), session.getCreatedAt()));
	}

	@Test
	void shouldDeleteExpiredSessionAfterRetentionPeriod() {
		User user = createUser();

		Instant now = clock.instant();

		RefreshSession session = createSession(user, now.minus(Duration.ofDays(3)));

		UUID sessionId = session.getId();

		cleanupService.cleanup();

		assertFalse(sessionRepository.existsById(sessionId));
	}

	@Test
	void shouldPreserveSessionWithinRetentionPeriod() {
		User user = createUser();

		Instant now = clock.instant();

		RefreshSession session = createSession(user, now.minus(Duration.ofHours(12)));

		UUID sessionId = session.getId();

		cleanupService.cleanup();

		assertTrue(sessionRepository.existsById(sessionId));
	}

	@Test
	void shouldPreserveNonExpiredSession() {
		User user = createUser();

		Instant now = clock.instant();

		RefreshSession session = createSession(user, now.plus(Duration.ofDays(7)));

		UUID sessionId = session.getId();

		cleanupService.cleanup();

		assertTrue(sessionRepository.existsById(sessionId));
	}

	@Test
	void shouldCascadeDeleteRefreshTokensWithSession() {
		User user = createUser();

		RefreshSession session = createSession(user, clock.instant().minus(Duration.ofDays(3)));

		RefreshToken token = createToken(session);

		UUID sessionId = session.getId();
		UUID tokenId = token.getId();

		assertTrue(tokenRepository.existsById(tokenId));

		cleanupService.cleanup();

		assertFalse(sessionRepository.existsById(sessionId));
		assertFalse(tokenRepository.existsById(tokenId));
	}

	@Test
	void shouldRespectCleanupBatchSize() {
		User user = createUser();

		Instant now = clock.instant();

		RefreshSession first = createSession(user, now.minus(Duration.ofDays(5)));

		RefreshSession second = createSession(user, now.minus(Duration.ofDays(4)));

		RefreshSession third = createSession(user, now.minus(Duration.ofDays(3)));

		List<UUID> sessionIds = List.of(first.getId(), second.getId(), third.getId());

		cleanupService.cleanup();

		long remaining = sessionIds.stream().filter(sessionRepository::existsById).count();

		assertEquals(1, remaining);

		cleanupService.cleanup();

		long remainingAfterSecondRun = sessionIds.stream().filter(sessionRepository::existsById).count();

		assertEquals(0, remainingAfterSecondRun);
	}

	@Test
	void shouldDeleteRevokedSessionOnlyAfterExpirationRetention() {
		User user = createUser();

		Instant now = clock.instant();

		RefreshSession expired = createSession(user, now.minus(Duration.ofDays(3)));

		expired.addRevokedAt(expired.getCreatedAt().plus(Duration.ofHours(1)));

		sessionRepository.saveAndFlush(expired);

		RefreshSession recent = createSession(user, now.plus(Duration.ofDays(2)));

		recent.addRevokedAt(now.minus(Duration.ofHours(1)));

		sessionRepository.saveAndFlush(recent);

		cleanupService.cleanup();

		assertFalse(sessionRepository.existsById(expired.getId()));
		assertTrue(sessionRepository.existsById(recent.getId()));
	}
}