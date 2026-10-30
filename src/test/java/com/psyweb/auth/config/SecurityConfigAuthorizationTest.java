
package com.psyweb.auth.config;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.service.UserService;

@WebMvcTest(controllers = SecurityTestAdminController.class, properties = "psyweb.security.cors.allowed-origins[0]=http://localhost:3000")
@Import({ SecurityConfig.class, SecurityTestAdminController.class,
		SecurityConfigAuthorizationTest.TestClockConfig.class })
@EnableWebSecurity
class SecurityConfigAuthorizationTest {

	private static final String ADMIN_URL = "/api/admin/test";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private User user(UserRole role, UserStatus status) {
		User mockUser = mock(User.class);

		when(mockUser.getRole()).thenReturn(role);
		when(mockUser.getStatus()).thenReturn(status);

		return mockUser;
	}

	private void stubUser(UserRole role, UserStatus status) {
		User mockUser = user(role, status);

		when(userService.getUser(10L)).thenReturn(mockUser);
	}

	// Successful authorization

	@Test
	void shouldAllowActiveAdmin() throws Exception {
		stubUser(UserRole.ADMIN, UserStatus.ACTIVE);

		mockMvc.perform(get(ADMIN_URL).with(jwt().jwt(token -> token.subject("10").claim("role", "ADMIN"))))
				.andExpect(status().isOk()).andExpect(content().string("ok"));

		verify(userService).getUser(10L);
	}

	// Role enforcement

	@ParameterizedTest
	@EnumSource(value = UserRole.class, names = { "CLIENT", "SPECIALIST" })
	void shouldRejectNonAdminRoles(UserRole role) throws Exception {
		stubUser(role, UserStatus.ACTIVE);

		mockMvc.perform(get(ADMIN_URL).with(jwt().jwt(token -> token.subject("10").claim("role", role.name()))))
				.andExpect(status().isForbidden());
	}

	// Account status enforcement

	@Test
	void shouldRejectBlockedAdmin() throws Exception {
		stubUser(UserRole.ADMIN, UserStatus.BLOCKED);

		mockMvc.perform(get(ADMIN_URL).with(jwt().jwt(token -> token.subject("10").claim("role", "ADMIN"))))
				.andExpect(status().isForbidden());
	}

	// Database role takes precedence over JWT role

	@Test
	void shouldRejectAdminJwtWhenDatabaseRoleIsClient() throws Exception {
		stubUser(UserRole.CLIENT, UserStatus.ACTIVE);

		mockMvc.perform(get(ADMIN_URL).with(jwt().jwt(token -> token.subject("10").claim("role", "ADMIN"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldAllowDatabaseAdminWithStaleJwtRole() throws Exception {
		stubUser(UserRole.ADMIN, UserStatus.ACTIVE);

		mockMvc.perform(get(ADMIN_URL).with(jwt().jwt(token -> token.subject("10").claim("role", "CLIENT"))))
				.andExpect(status().isOk()).andExpect(content().string("ok"));
	}

	// Authentication enforcement

	@Test
	void shouldRejectAnonymousRequest() throws Exception {
		mockMvc.perform(get(ADMIN_URL)).andExpect(status().isUnauthorized());

		verifyNoInteractions(userService);
	}

	// JWT subject validation

	@Test
	void shouldRejectNonNumericSubject() throws Exception {
		mockMvc.perform(get(ADMIN_URL).with(jwt().jwt(token -> token.subject("invalid").claim("role", "ADMIN"))))
				.andExpect(status().isForbidden());

		verifyNoInteractions(userService);
	}

	@Test
	void shouldRejectZeroSubject() throws Exception {
		mockMvc.perform(get(ADMIN_URL).with(jwt().jwt(token -> token.subject("0").claim("role", "ADMIN"))))
				.andExpect(status().isForbidden());

		verifyNoInteractions(userService);
	}

	@Test
	void shouldRejectNegativeSubject() throws Exception {
		mockMvc.perform(get(ADMIN_URL).with(jwt().jwt(token -> token.subject("-5").claim("role", "ADMIN"))))
				.andExpect(status().isForbidden());

		verifyNoInteractions(userService);
	}

	// Unconfigured endpoints remain denied

	@Test
	void shouldRejectUnconfiguredEndpointForAdmin() throws Exception {
		mockMvc.perform(get("/api/unknown").with(jwt().jwt(token -> token.subject("10").claim("role", "ADMIN"))))
				.andExpect(status().isForbidden());

		verifyNoInteractions(userService);
	}

	// Test infrastructure

	@TestConfiguration(proxyBeanMethods = false)
	static class TestClockConfig {

		@Bean
		Clock clock() {
			return Clock.systemUTC();
		}
	}
}

// Test-only controller. Not part of production application.

@RestController
class SecurityTestAdminController {

	@GetMapping("/api/admin/test")
	public ResponseEntity<String> testAdmin() {
		return ResponseEntity.ok("ok");
	}
}