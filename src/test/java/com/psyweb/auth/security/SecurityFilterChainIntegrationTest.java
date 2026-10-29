package com.psyweb.auth.security;

import com.psyweb.auth.config.JwtProperties;
import com.psyweb.auth.service.AccessTokenService;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@AutoConfigureMockMvc
@Import(TestSecurityController.class)
class SecurityFilterChainIntegrationTest extends PostgreSQLIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccessTokenService accessTokenService;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private JwtProperties jwtProperties;

	@Autowired
	private Clock clock;

	@Test
	void shouldReturnUnauthorizedWhenTokenIsMissing() throws Exception {
		mockMvc.perform(get("/api/admin/security-test")).andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", startsWith("Bearer")))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.message").value("Authentication required"))
				.andExpect(jsonPath("$.path").value("/api/admin/security-test"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void shouldReturnUnauthorizedWhenTokenIsInvalid() throws Exception {
		mockMvc.perform(get("/api/admin/security-test").header("Authorization", "Bearer invalid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate",
						allOf(startsWith("Bearer"), containsString("error=\"invalid_token\""))))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.message").value("Authentication required"))
				.andExpect(jsonPath("$.path").value("/api/admin/security-test"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void shouldDenyAccessToUnconfiguredEndpoint() throws Exception {
		mockMvc.perform(get("/api/security-unconfigured")).andExpect(status().isUnauthorized());
	}

	@Test
	void shouldAllowActiveAdminWithValidJwt() throws Exception {
		User admin = new User("security-" + UUID.randomUUID() + "@example.com", "test-password-hash", UserRole.ADMIN,
				UserStatus.ACTIVE);

		admin = userRepository.saveAndFlush(admin);

		String token = accessTokenService.generateAccessToken(admin.getId(), admin.getRole()).accessToken();

		mockMvc.perform(get("/api/admin/security-test").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(content().string("OK"));
	}

	@Test
	void shouldDenyActiveClientAccessToAdminEndpoint() throws Exception {
		User client = userRepository.saveAndFlush(new User("security-" + UUID.randomUUID() + "@example.com",
				"test-password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		String token = accessTokenService.generateAccessToken(client.getId(), client.getRole()).accessToken();

		mockMvc.perform(get("/api/admin/security-test").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"))
				.andExpect(jsonPath("$.message").value("Access denied"));
	}

	@Test
	void shouldDenyBlockedAdminWithValidJwt() throws Exception {
		User admin = userRepository.saveAndFlush(new User("security-" + UUID.randomUUID() + "@example.com",
				"test-password-hash", UserRole.ADMIN, UserStatus.BLOCKED));

		String token = accessTokenService.generateAccessToken(admin.getId(), admin.getRole()).accessToken();

		mockMvc.perform(get("/api/admin/security-test").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void shouldRevokeAccessAfterUserBecomesBlocked() throws Exception {
		User admin = userRepository.saveAndFlush(new User("security-" + UUID.randomUUID() + "@example.com",
				"test-password-hash", UserRole.ADMIN, UserStatus.ACTIVE));

		String token = accessTokenService.generateAccessToken(admin.getId(), admin.getRole()).accessToken();

		mockMvc.perform(get("/api/admin/security-test").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		admin.block();
		userRepository.saveAndFlush(admin);

		mockMvc.perform(get("/api/admin/security-test").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void shouldDenyUnconfiguredEndpointEvenWithValidJwt() throws Exception {
		User admin = userRepository.saveAndFlush(new User("security-" + UUID.randomUUID() + "@example.com",
				"test-password-hash", UserRole.ADMIN, UserStatus.ACTIVE));

		String token = accessTokenService.generateAccessToken(admin.getId(), admin.getRole()).accessToken();

		mockMvc.perform(get("/api/security-unconfigured").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldAllowPublicRegistrationWithoutJwt() throws Exception {
		mockMvc.perform(post("/api/auth/register/client").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturnUnauthorizedWhenJwtIsExpired() throws Exception {
		Instant now = Instant.now(clock);

		Instant issuedAt = now.minus(jwtProperties.accessTokenTtl()).minusSeconds(120);

		Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl());

		JwtClaimsSet claims = JwtClaimsSet.builder().issuer(jwtProperties.issuer()).subject("1")
				.audience(List.of(jwtProperties.audience())).claim("role", "ADMIN").issuedAt(issuedAt)
				.expiresAt(expiresAt).build();

		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(jwtProperties.keyId()).build();

		String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

		mockMvc.perform(get("/api/admin/security-test").header("Authorization", "Bearer " + expiredToken))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate",
						allOf(startsWith("Bearer"), containsString("error=\"invalid_token\""))))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.message").value("Authentication required"));
	}

	@Test
	void shouldDenyValidJwtWhenUserDoesNotExist() throws Exception {
		User user = userRepository.saveAndFlush(new User("security-" + UUID.randomUUID() + "@example.com",
				"test-password-hash", UserRole.ADMIN, UserStatus.ACTIVE));

		String token = accessTokenService.generateAccessToken(user.getId(), user.getRole()).accessToken();

		userRepository.delete(user);
		userRepository.flush();

		mockMvc.perform(get("/api/admin/security-test").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void shouldRequireAuthenticationForPostWithoutCsrfToken() throws Exception {
		mockMvc.perform(post("/api/admin/security-test")).andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", startsWith("Bearer")))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void shouldAllowAuthenticatedAdminPostWithoutCsrfToken() throws Exception {
		User admin = userRepository.saveAndFlush(new User("security-" + UUID.randomUUID() + "@example.com",
				"test-password-hash", UserRole.ADMIN, UserStatus.ACTIVE));

		String token = accessTokenService.generateAccessToken(admin.getId(), admin.getRole()).accessToken();

		mockMvc.perform(post("/api/admin/security-test").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(content().string("POST_OK"));
	}

	@ParameterizedTest
	@CsvSource({ "X-Content-Type-Options, nosniff", "X-Frame-Options, DENY",
			"Referrer-Policy, strict-origin-when-cross-origin",
			"Content-Security-Policy, default-src 'none'; frame-ancestors 'none'" })
	void shouldReturnSecurityHeaders(String headerName, String expectedValue) throws Exception {

		mockMvc.perform(get("/api/admin/security-test")).andExpect(status().isUnauthorized())
				.andExpect(header().string(headerName, expectedValue));
	}

	@Test
	void shouldReturnSecurityHeadersForPublicEndpoint() throws Exception {
		mockMvc.perform(post("/api/auth/register/client").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest()).andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
				.andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"));
	}
}