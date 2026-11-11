
package com.psyweb.user.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.UUID;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.psyweb.auth.service.AccessTokenService;
import com.psyweb.client.domain.Client;
import com.psyweb.client.repository.ClientRepository;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;
import com.psyweb.specialist.domain.Specialist;

@AutoConfigureMockMvc
class MeSecurityIntegrationTest extends PostgreSQLIntegrationTest {

	private static final String ME_URL = "/api/me";
	private static final String ME_PROFILE_URL = "/api/me/profile";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private AccessTokenService accessTokenService;

	@Autowired
	private SpecialistRepository specialistRepository;

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

	@Test
	void shouldUpdateClientProfileAndPersistChanges() throws Exception {
		User user = createUser(UserStatus.ACTIVE);
		clientRepository.saveAndFlush(new Client(user, "Anna", "Smith"));

		mockMvc.perform(
				patch(ME_PROFILE_URL).with(csrf()).header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken(user))
						.contentType(MediaType.APPLICATION_JSON).content("""
								{"firstName": "Maria"}
								"""))
				.andExpect(status().isOk()).andExpect(jsonPath("$.account.id").value(user.getId()))
				.andExpect(jsonPath("$.profile.firstName").value("Maria"))
				.andExpect(jsonPath("$.profile.lastName").value("Smith"))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));

		Client saved = clientRepository.findById(user.getId()).orElseThrow();

		assertEquals("Maria", saved.getFirstName());
		assertEquals("Smith", saved.getLastName());
	}

	@Test
	void shouldUpdateBothClientProfileFields() throws Exception {
		User user = createUser(UserStatus.ACTIVE);
		clientRepository.saveAndFlush(new Client(user, "Anna", "Smith"));

		mockMvc.perform(
				patch(ME_PROFILE_URL).with(csrf()).header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken(user))
						.contentType(MediaType.APPLICATION_JSON).content("""
								{
								    "firstName": "Maria",
								    "lastName": "Jones"
								}
								"""))
				.andExpect(status().isOk()).andExpect(jsonPath("$.profile.firstName").value("Maria"))
				.andExpect(jsonPath("$.profile.lastName").value("Jones"));

		Client saved = clientRepository.findById(user.getId()).orElseThrow();

		assertEquals("Maria", saved.getFirstName());
		assertEquals("Jones", saved.getLastName());
	}

	@Test
	void shouldAllowPendingSpecialistToUpdateProfile() throws Exception {
		User user = userRepository.saveAndFlush(new User("specialist-" + UUID.randomUUID() + "@example.com",
				"test-password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		specialistRepository.saveAndFlush(new Specialist(user, "Kate", "Jones", Duration.ZERO, Duration.ZERO));

		mockMvc.perform(
				patch(ME_PROFILE_URL).with(csrf()).header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken(user))
						.contentType(MediaType.APPLICATION_JSON).content("""
								{"firstName": "Maria"}
								"""))
				.andExpect(status().isOk()).andExpect(jsonPath("$.profile.firstName").value("Maria"))
				.andExpect(jsonPath("$.profile.lastName").value("Jones"))
				.andExpect(jsonPath("$.profile.approvalStatus").value("PENDING"));

		Specialist saved = specialistRepository.findById(user.getId()).orElseThrow();

		assertEquals("Maria", saved.getFirstName());
		assertEquals("Jones", saved.getLastName());
	}

	@Test
	void shouldNotUpdateAnotherClientProfile() throws Exception {
		User actor = createUser(UserStatus.ACTIVE);
		User other = createUser(UserStatus.ACTIVE);

		clientRepository.saveAndFlush(new Client(actor, "Anna", "Smith"));
		clientRepository.saveAndFlush(new Client(other, "Kate", "Jones"));

		mockMvc.perform(
				patch(ME_PROFILE_URL).with(csrf()).header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken(actor))
						.contentType(MediaType.APPLICATION_JSON).content("""
								{
								    "firstName": "Maria",
								    "clientId": %d
								}
								""".formatted(other.getId())))
				.andExpect(status().isBadRequest());

		Client actorProfile = clientRepository.findById(actor.getId()).orElseThrow();
		Client otherProfile = clientRepository.findById(other.getId()).orElseThrow();

		assertEquals("Anna", actorProfile.getFirstName());
		assertEquals("Kate", otherProfile.getFirstName());
	}

	@Test
	void shouldReturn401WhenJwtIsMissingOnProfileUpdate() throws Exception {
		mockMvc.perform(patch(ME_PROFILE_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
				{"firstName": "Maria"}
				""")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void shouldReturn401WhenJwtIsInvalidOnProfileUpdate() throws Exception {
		mockMvc.perform(patch(ME_PROFILE_URL).with(csrf()).header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"firstName": "Maria"}
						""")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void shouldRejectBlockedClientProfileUpdate() throws Exception {
		User user = createUser(UserStatus.BLOCKED);
		clientRepository.saveAndFlush(new Client(user, "Anna", "Smith"));

		mockMvc.perform(
				patch(ME_PROFILE_URL).with(csrf()).header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken(user))
						.contentType(MediaType.APPLICATION_JSON).content("""
								{"firstName": "Maria"}
								"""))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("USER_ACCESS_FORBIDDEN"));

		Client saved = clientRepository.findById(user.getId()).orElseThrow();

		assertEquals("Anna", saved.getFirstName());
	}

	@Test
	void shouldAllowAuthenticatedProfileUpdateWithoutCsrfToken() throws Exception {
		User user = createUser(UserStatus.ACTIVE);
		clientRepository.saveAndFlush(new Client(user, "Anna", "Smith"));

		mockMvc.perform(patch(ME_PROFILE_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken(user))
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"firstName": "Maria"}
						""")).andExpect(status().isOk()).andExpect(jsonPath("$.profile.firstName").value("Maria"));

		Client saved = clientRepository.findById(user.getId()).orElseThrow();

		assertEquals("Maria", saved.getFirstName());
	}

	@Test
	void shouldRejectInvalidProfileFieldWithoutChangingData() throws Exception {
		User user = createUser(UserStatus.ACTIVE);
		clientRepository.saveAndFlush(new Client(user, "Anna", "Smith"));

		mockMvc.perform(
				patch(ME_PROFILE_URL).with(csrf()).header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken(user))
						.contentType(MediaType.APPLICATION_JSON).content("""
								{
								    "firstName": "Maria",
								    "role": "ADMIN"
								}
								"""))
				.andExpect(status().isBadRequest());

		Client saved = clientRepository.findById(user.getId()).orElseThrow();

		assertEquals("Anna", saved.getFirstName());
		assertEquals("Smith", saved.getLastName());
	}

	@Test
	void shouldRejectBlankFirstNameWithoutChangingData() throws Exception {
		User user = createUser(UserStatus.ACTIVE);
		clientRepository.saveAndFlush(new Client(user, "Anna", "Smith"));

		mockMvc.perform(
				patch(ME_PROFILE_URL).with(csrf()).header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken(user))
						.contentType(MediaType.APPLICATION_JSON).content("""
								{"firstName": "   "}
								"""))
				.andExpect(status().isBadRequest());

		Client saved = clientRepository.findById(user.getId()).orElseThrow();

		assertEquals("Anna", saved.getFirstName());
	}
}