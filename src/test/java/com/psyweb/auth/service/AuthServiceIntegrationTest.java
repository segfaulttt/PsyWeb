
package com.psyweb.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.psyweb.auth.domain.RefreshSession;
import com.psyweb.auth.repository.RefreshSessionRepository;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.psyweb.auth.service.dto.AuthenticationResult;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.domain.SpecialistStatus;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.user.exception.UserAccessForbiddenException;

class AuthServiceIntegrationTest extends PostgreSQLIntegrationTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshSessionRepository sessionRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private SpecialistRepository specialistRepository;

	@MockitoSpyBean
	private AccessTokenService accessTokenService;

	@Test
	void shouldRollbackRefreshSessionWhenAccessTokenGenerationFails() {

		String email = "auth-rollback-" + UUID.randomUUID() + "@example.com";
		String rawPassword = "test-password";

		User user = userRepository
				.saveAndFlush(new User(email, passwordEncoder.encode(rawPassword), UserRole.CLIENT, UserStatus.ACTIVE));

		Long userId = user.getId();

		assertTrue(sessionRepository.findByUserId(userId).isEmpty());

		doThrow(new IllegalStateException("JWT signing failed")).when(accessTokenService)
				.generateAccessToken(eq(userId), eq(UserRole.CLIENT));

		assertThrows(IllegalStateException.class, () -> authService.login(email, rawPassword));

		verify(accessTokenService).generateAccessToken(userId, UserRole.CLIENT);

		List<RefreshSession> sessions = sessionRepository.findByUserId(userId);

		assertTrue(sessions.isEmpty(), "Refresh session must not persist when JWT generation fails");
	}

	@ParameterizedTest
	@EnumSource(SpecialistStatus.class)
	void shouldAllowActiveSpecialistToLoginRegardlessOfApprovalStatus(SpecialistStatus specialistStatus) {

		String email = "active-specialist-" + UUID.randomUUID() + "@example.com";
		String rawPassword = "test-password";

		User user = userRepository.saveAndFlush(
				new User(email, passwordEncoder.encode(rawPassword), UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = new Specialist(user, "Anna", "Test", Duration.ZERO, Duration.ZERO);

		switch (specialistStatus) {
		case PENDING -> {
		}
		case APPROVED -> specialist.approve();
		case REJECTED -> specialist.reject();
		case SUSPENDED -> {
			specialist.approve();
			specialist.suspend();
		}
		}

		specialistRepository.saveAndFlush(specialist);

		AuthenticationResult result = authService.login(email, rawPassword);

		assertNotNull(result.accessToken());
		assertNotNull(result.refreshToken());
		assertNotNull(result.expiresAt());

		assertEquals(UserRole.SPECIALIST, accessTokenService.verifyAccessToken(result.accessToken()).role());

		assertEquals(user.getId(), accessTokenService.verifyAccessToken(result.accessToken()).userId());

		assertEquals(specialistStatus, specialistRepository.findById(user.getId()).orElseThrow().getApprovalStatus());

		assertEquals(1, sessionRepository.findByUserId(user.getId()).size());
	}

	@ParameterizedTest
	@EnumSource(SpecialistStatus.class)
	void shouldRejectBlockedSpecialistRegardlessOfApprovalStatus(SpecialistStatus specialistStatus) {

		String email = "blocked-specialist-" + UUID.randomUUID() + "@example.com";
		String rawPassword = "test-password";

		User user = userRepository.saveAndFlush(
				new User(email, passwordEncoder.encode(rawPassword), UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = new Specialist(user, "Anna", "Blocked", Duration.ZERO, Duration.ZERO);

		switch (specialistStatus) {
		case PENDING -> {
		}
		case APPROVED -> specialist.approve();
		case REJECTED -> specialist.reject();
		case SUSPENDED -> {
			specialist.approve();
			specialist.suspend();
		}
		}

		specialistRepository.saveAndFlush(specialist);

		user.block();
		userRepository.saveAndFlush(user);

		assertThrows(UserAccessForbiddenException.class, () -> authService.login(email, rawPassword));

		assertEquals(0, sessionRepository.findByUserId(user.getId()).size());

		assertEquals(UserStatus.BLOCKED, userRepository.findById(user.getId()).orElseThrow().getStatus());
	}

}