
package com.psyweb.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.psyweb.auth.domain.RefreshSession;
import com.psyweb.auth.repository.RefreshSessionRepository;
import com.psyweb.auth.service.dto.IssuedRefreshToken;
import com.psyweb.auth.service.dto.RefreshRotationResult;
import com.psyweb.auth.service.dto.ReplayDetected;
import com.psyweb.auth.service.dto.Success;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

class RefreshSessionConcurrencyIntegrationTest extends PostgreSQLIntegrationTest {

	@Autowired
	private RefreshSessionService refreshSessionService;

	@Autowired
	private RefreshSessionRepository sessionRepository;

	@Autowired
	private UserRepository userRepository;

	private User createUser() {
		return userRepository.saveAndFlush(new User("concurrency-" + UUID.randomUUID() + "@example.com",
				"password-hash", UserRole.CLIENT, UserStatus.ACTIVE));
	}

	private RefreshSession getSession(Long userId) {
		List<RefreshSession> sessions = sessionRepository.findByUserId(userId);

		assertEquals(1, sessions.size());

		return sessions.getFirst();
	}

	private static void awaitStart(CountDownLatch start) throws InterruptedException {
		if (!start.await(10, TimeUnit.SECONDS)) {
			throw new AssertionError("Concurrent test start timeout");
		}
	}

	@Test
	void shouldAllowOnlyOneSuccessfulRotationForSameToken() throws Exception {

		User user = createUser();

		IssuedRefreshToken initial = refreshSessionService.createSession(user.getId());

		String rawToken = initial.rawRefreshToken();

		CountDownLatch start = new CountDownLatch(1);

		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Future<RefreshRotationResult> first = executor.submit(() -> {
				awaitStart(start);
				return refreshSessionService.rotation(rawToken);
			});

			Future<RefreshRotationResult> second = executor.submit(() -> {
				awaitStart(start);
				return refreshSessionService.rotation(rawToken);
			});

			start.countDown();

			RefreshRotationResult firstResult = first.get(30, TimeUnit.SECONDS);

			RefreshRotationResult secondResult = second.get(30, TimeUnit.SECONDS);

			long successCount = List.of(firstResult, secondResult).stream().filter(Success.class::isInstance).count();

			long replayCount = List.of(firstResult, secondResult).stream().filter(ReplayDetected.class::isInstance)
					.count();

			assertEquals(1, successCount);
			assertEquals(1, replayCount);

			RefreshSession session = getSession(user.getId());

			assertNotNull(session.getRevokedAt());

			Success success = firstResult instanceof Success s ? s : (Success) secondResult;

			assertThrows(IllegalArgumentException.class,
					() -> refreshSessionService.rotation(success.issuedRefreshToken().rawRefreshToken()));

		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void shouldKeepSessionRevokedWhenRotationRacesWithLogout() throws Exception {

		User user = createUser();

		IssuedRefreshToken initial = refreshSessionService.createSession(user.getId());

		String rawToken = initial.rawRefreshToken();

		CountDownLatch start = new CountDownLatch(1);

		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Future<Object> rotationFuture = executor.submit(() -> {
				awaitStart(start);

				try {
					return refreshSessionService.rotation(rawToken);
				} catch (IllegalArgumentException exception) {
					return exception;
				}
			});

			Future<Void> logoutFuture = executor.submit(() -> {
				awaitStart(start);
				refreshSessionService.logout(rawToken);
				return null;
			});

			start.countDown();

			Object rotationResult = rotationFuture.get(30, TimeUnit.SECONDS);

			logoutFuture.get(30, TimeUnit.SECONDS);

			RefreshSession session = getSession(user.getId());

			assertNotNull(session.getRevokedAt());

			assertTrue(rotationResult instanceof Success || rotationResult instanceof IllegalArgumentException,
					"Rotation must either succeed before logout or be rejected");

			if (rotationResult instanceof Success success) {
				assertThrows(IllegalArgumentException.class,
						() -> refreshSessionService.rotation(success.issuedRefreshToken().rawRefreshToken()));
			}

		} finally {
			executor.shutdownNow();
		}
	}
}