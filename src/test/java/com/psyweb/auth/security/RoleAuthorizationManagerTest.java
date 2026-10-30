
package com.psyweb.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.psyweb.auth.authorization.AuthorizationPolicy;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.exception.UserNotFoundException;
import com.psyweb.user.service.UserService;

class RoleAuthorizationManagerTest {

	private final UserService userService = mock(UserService.class);
	private final AuthorizationPolicy policy = new AuthorizationPolicy();

	private RoleAuthorizationManager manager(UserRole role) {
		return new RoleAuthorizationManager(userService, policy, role);
	}

	private User user(UserRole role, UserStatus status) {
		User user = mock(User.class);
		when(user.getRole()).thenReturn(role);
		when(user.getStatus()).thenReturn(status);
		return user;
	}

	private Supplier<Authentication> jwtAuthentication(String subject, String tokenRole) {
		Jwt.Builder builder = Jwt.withTokenValue("test-token").header("alg", "RS256").claim("role", tokenRole);

		if (subject != null) {
			builder.subject(subject);
		} else {
			builder.claim("sub", "");
		}

		Jwt jwt = builder.build();
		return () -> new JwtAuthenticationToken(jwt);
	}

	private boolean granted(RoleAuthorizationManager manager, Supplier<? extends Authentication> authentication) {
		AuthorizationResult result = manager.authorize(authentication, null);
		assertNotNull(result, "AuthorizationManager must return an explicit decision");
		return result.isGranted();
	}

	// Successful authorization

	@ParameterizedTest
	@EnumSource(UserRole.class)
	void shouldAllowUserWithRequiredRoleAndActiveStatus(UserRole role) {
		User mockUser = user(role, UserStatus.ACTIVE);

		when(userService.getUser(10L)).thenReturn(mockUser);

		assertTrue(granted(manager(role), jwtAuthentication("10", role.name())));
	}

	// Authentication validation

	@Test
	void shouldRejectNullAuthenticationSupplier() {
		assertFalse(granted(manager(UserRole.CLIENT), null));
		verifyNoInteractions(userService);
	}

	@Test
	void shouldRejectNullAuthentication() {
		assertFalse(granted(manager(UserRole.CLIENT), () -> null));
		verifyNoInteractions(userService);
	}

	@Test
	void shouldRejectNonJwtAuthentication() {
		Authentication authentication = mock(Authentication.class);

		assertFalse(granted(manager(UserRole.CLIENT), () -> authentication));
		verifyNoInteractions(userService);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "abc", "0", "-1", "999999999999999999999" })
	void shouldRejectInvalidJwtSubject(String subject) {
		assertFalse(granted(manager(UserRole.CLIENT), jwtAuthentication(subject, "CLIENT")));

		verifyNoInteractions(userService);
	}

	// User lookup

	@Test
	void shouldRejectMissingUser() {
		when(userService.getUser(10L)).thenThrow(new UserNotFoundException("User not found"));

		assertFalse(granted(manager(UserRole.CLIENT), jwtAuthentication("10", "CLIENT")));
	}

	// Role and status enforcement

	@ParameterizedTest
	@EnumSource(UserRole.class)
	void shouldRejectBlockedUser(UserRole role) {
		User mockUser = user(role, UserStatus.BLOCKED);

		when(userService.getUser(10L)).thenReturn(mockUser);

		assertFalse(granted(manager(role), jwtAuthentication("10", role.name())));
	}

	@Test
	void shouldRejectClientForAdminEndpoint() {
		User client = user(UserRole.CLIENT, UserStatus.ACTIVE);

		when(userService.getUser(10L)).thenReturn(client);

		assertFalse(granted(manager(UserRole.ADMIN), jwtAuthentication("10", "CLIENT")));
	}

	@Test
	void shouldRejectSpecialistForClientEndpoint() {
		User specialist = user(UserRole.SPECIALIST, UserStatus.ACTIVE);

		when(userService.getUser(10L)).thenReturn(specialist);

		assertFalse(granted(manager(UserRole.CLIENT), jwtAuthentication("10", "SPECIALIST")));
	}

	@Test
	void shouldRejectAdminForSpecialistEndpoint() {
		User admin = user(UserRole.ADMIN, UserStatus.ACTIVE);

		when(userService.getUser(10L)).thenReturn(admin);

		assertFalse(granted(manager(UserRole.SPECIALIST), jwtAuthentication("10", "ADMIN")));
	}

	// JWT role must not override database role

	@Test
	void shouldRejectAdminJwtWhenDatabaseRoleIsClient() {
		User client = user(UserRole.CLIENT, UserStatus.ACTIVE);

		when(userService.getUser(10L)).thenReturn(client);

		assertFalse(granted(manager(UserRole.ADMIN), jwtAuthentication("10", "ADMIN")));
	}

	@Test
	void shouldAllowDatabaseAdminDespiteStaleJwtRole() {
		User admin = user(UserRole.ADMIN, UserStatus.ACTIVE);

		when(userService.getUser(10L)).thenReturn(admin);

		assertTrue(granted(manager(UserRole.ADMIN), jwtAuthentication("10", "CLIENT")));
	}

	// Configuration validation

	@Test
	void shouldRejectNullUserService() {
		assertThrows(NullPointerException.class, () -> new RoleAuthorizationManager(null, policy, UserRole.CLIENT));
	}

	@Test
	void shouldRejectNullAuthorizationPolicy() {
		assertThrows(NullPointerException.class,
				() -> new RoleAuthorizationManager(userService, null, UserRole.CLIENT));
	}

	@Test
	void shouldRejectNullRequiredRole() {
		assertThrows(NullPointerException.class, () -> new RoleAuthorizationManager(userService, policy, null));
	}
}