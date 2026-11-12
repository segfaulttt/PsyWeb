package com.psyweb.user.web.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.psyweb.auth.service.AccessTokenService;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

@AutoConfigureMockMvc
class MePasswordSecurityIntegrationTest extends PostgreSQLIntegrationTest {

	private static final String URL = "/api/me/password";
	private static final String OLD_PASSWORD = "CurrentPassword123!";
	private static final String NEW_PASSWORD = "NewStrongPassword123!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccessTokenService accessTokenService;

	private User createUser(UserStatus status) {
		return userRepository.saveAndFlush(new User("password-security-" + UUID.randomUUID() + "@example.com",
				passwordEncoder.encode(OLD_PASSWORD), UserRole.CLIENT, status));
	}

	private String accessToken(User user) {
		return accessTokenService.generateAccessToken(user.getId(), user.getRole()).accessToken();
	}

	private String validRequest() {
		return """
				{
				    "currentPassword": "CurrentPassword123!",
				    "newPassword": "NewStrongPassword123!"
				}
				""";
	}

	@Test
	void shouldRejectPasswordChangeWithoutAuthentication() throws Exception {

		mockMvc.perform(patch(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(validRequest()))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void shouldRejectPasswordChangeWithInvalidJwt() throws Exception {

		mockMvc.perform(patch(URL).with(csrf()).header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
				.contentType(MediaType.APPLICATION_JSON).content(validRequest())).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void shouldChangePasswordWithValidJwt() throws Exception {
		User user = createUser(UserStatus.ACTIVE);

		mockMvc.perform(patch(URL).with(csrf()).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(user))
				.contentType(MediaType.APPLICATION_JSON).content(validRequest())).andExpect(status().isNoContent())
				.andExpect(content().string("")).andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

		User updated = userRepository.findById(user.getId()).orElseThrow();

		assertTrue(passwordEncoder.matches(NEW_PASSWORD, updated.getPasswordHash()));

		assertFalse(passwordEncoder.matches(OLD_PASSWORD, updated.getPasswordHash()));
	}

	@Test
	void shouldNotChangeAnotherUsersPassword() throws Exception {

		User actor = createUser(UserStatus.ACTIVE);
		User other = createUser(UserStatus.ACTIVE);

		String otherOriginalHash = other.getPasswordHash();

		mockMvc.perform(patch(URL).with(csrf()).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(actor))
				.contentType(MediaType.APPLICATION_JSON).content("""
						{
						    "userId": %d,
						    "currentPassword": "CurrentPassword123!",
						    "newPassword": "NewStrongPassword123!"
						}
						""".formatted(other.getId()))).andExpect(status().isNoContent());

		User updatedActor = userRepository.findById(actor.getId()).orElseThrow();

		User unchangedOther = userRepository.findById(other.getId()).orElseThrow();

		assertTrue(passwordEncoder.matches(NEW_PASSWORD, updatedActor.getPasswordHash()));

		assertEquals(otherOriginalHash, unchangedOther.getPasswordHash());

		assertTrue(passwordEncoder.matches(OLD_PASSWORD, unchangedOther.getPasswordHash()));
	}

	@Test
	void shouldRejectPasswordChangeForBlockedUser() throws Exception {

		User user = createUser(UserStatus.BLOCKED);
		String originalHash = user.getPasswordHash();

		mockMvc.perform(patch(URL).with(csrf()).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(user))
				.contentType(MediaType.APPLICATION_JSON).content(validRequest())).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("USER_ACCESS_FORBIDDEN"));

		User persisted = userRepository.findById(user.getId()).orElseThrow();

		assertEquals(originalHash, persisted.getPasswordHash());
	}

	@Test
	void shouldRejectIncorrectCurrentPassword() throws Exception {

		User user = createUser(UserStatus.ACTIVE);
		String originalHash = user.getPasswordHash();

		mockMvc.perform(patch(URL).with(csrf()).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(user))
				.contentType(MediaType.APPLICATION_JSON).content("""
						{
						    "currentPassword": "IncorrectPassword123!",
						    "newPassword": "NewStrongPassword123!"
						}
						""")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"))
				.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

		User persisted = userRepository.findById(user.getId()).orElseThrow();

		assertEquals(originalHash, persisted.getPasswordHash());
	}

	@Test
	void shouldRejectBlankNewPasswordBeforeServiceExecution() throws Exception {

		User user = createUser(UserStatus.ACTIVE);
		String originalHash = user.getPasswordHash();

		mockMvc.perform(patch(URL).with(csrf()).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(user))
				.contentType(MediaType.APPLICATION_JSON).content("""
						{
						    "currentPassword": "CurrentPassword123!",
						    "newPassword": " "
						}
						""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.newPassword").isArray());

		User persisted = userRepository.findById(user.getId()).orElseThrow();

		assertEquals(originalHash, persisted.getPasswordHash());
	}
}