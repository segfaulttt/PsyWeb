
package com.psyweb.user.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import com.psyweb.auth.service.AccessTokenService;
import com.psyweb.client.domain.Client;
import com.psyweb.client.repository.ClientRepository;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

@AutoConfigureMockMvc
class MeSecurityIntegrationTest extends PostgreSQLIntegrationTest {

	private static final String ME_URL = "/api/me";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private AccessTokenService accessTokenService;

	@Test
	void shouldReturn401WhenJwtIsMissing() throws Exception {
		mockMvc.perform(get(ME_URL)).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED")).andExpect(jsonPath("$.path").value(ME_URL))
				.andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE));
	}

	@Test
	void shouldReturn401WhenJwtIsInvalid() throws Exception {
		mockMvc.perform(get(ME_URL).header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.path").value(ME_URL));
	}

	@Test
	void shouldReturnCurrentProfileWithValidJwt() throws Exception {
		User user = createUser(UserStatus.ACTIVE);

		Client client = new Client(user, "Anna", "Smith");
		clientRepository.saveAndFlush(client);

		String token = createToken(user);

		mockMvc.perform(get(ME_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andExpect(status().isOk())
				.andExpect(jsonPath("$.account.id").value(user.getId()))
				.andExpect(jsonPath("$.account.email").value(user.getEmail()))
				.andExpect(jsonPath("$.account.role").value("CLIENT"))
				.andExpect(jsonPath("$.profile.id").value(user.getId()))
				.andExpect(jsonPath("$.profile.firstName").value("Anna"))
				.andExpect(jsonPath("$.profile.lastName").value("Smith"))
				.andExpect(jsonPath("$.profile.approvalStatus").doesNotExist())
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
	}

	@Test
	void shouldReturn403WhenUserIsBlocked() throws Exception {
		User user = createUser(UserStatus.BLOCKED);

		String token = createToken(user);

		mockMvc.perform(get(ME_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("USER_ACCESS_FORBIDDEN"));
	}

	@Test
	void shouldRejectUnsupportedHttpMethodWithValidJwt() throws Exception {

		User user = createUser(UserStatus.ACTIVE);
		String token = createToken(user);

		mockMvc.perform(post(ME_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isForbidden());
	}

	private User createUser(UserStatus status) {
		User user = new User("me-test-" + UUID.randomUUID() + "@example.com", "test-password-hash", UserRole.CLIENT,
				status);

		return userRepository.saveAndFlush(user);
	}

	private String createToken(User user) {
		return accessTokenService.generateAccessToken(user.getId(), user.getRole()).accessToken();
	}
}