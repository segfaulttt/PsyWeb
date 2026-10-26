
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

class AuthServiceIntegrationTest extends PostgreSQLIntegrationTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshSessionRepository sessionRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

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
}