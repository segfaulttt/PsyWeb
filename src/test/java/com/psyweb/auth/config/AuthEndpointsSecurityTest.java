package com.psyweb.auth.config;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.psyweb.auth.authorization.CurrentActorProvider;
import com.psyweb.auth.ratelimit.AuthRateLimiter;
import com.psyweb.auth.ratelimit.RateLimitDecision;
import com.psyweb.auth.ratelimit.RateLimitPolicy;
import com.psyweb.auth.service.AuthService;
import com.psyweb.auth.service.PasswordResetService;
import com.psyweb.auth.service.RegistrationService;
import com.psyweb.auth.web.controller.AuthController;
import com.psyweb.auth.web.cookie.RefreshCookieFactory;
import com.psyweb.auth.web.mapper.AuthWebMapper;
import com.psyweb.auth.web.mapper.RegistrationWebMapper;
import com.psyweb.user.service.UserService;

@WebMvcTest(controllers = AuthController.class, properties = {
		"psyweb.security.cors.allowed-origins[0]=http://localhost:3000" })
@Import({ SecurityConfig.class, AuthEndpointsSecurityTest.TestClockConfig.class })
@EnableWebSecurity
class AuthEndpointsSecurityTest {

	private static final String FORBIDDEN_ORIGIN = "https://evil.example.com";

	private static final String LOGIN_URL = "/api/auth/login";

	private static final String RESET_REQUEST_URL = "/api/auth/password-reset/request";

	private static final String RESET_CONFIRM_URL = "/api/auth/password-reset/confirm";

	private static final String RESET_REQUEST_JSON = """
			{
			    "email": "anna@example.com"
			}
			""";

	private static final String RESET_CONFIRM_JSON = """
			{
			    "token": "valid-reset-token",
			    "newPassword": "NewStrongPassword123!"
			}
			""";

	private static final RateLimitPolicy RESET_REQUEST_POLICY = new RateLimitPolicy(5, Duration.ofMinutes(15));

	private static final RateLimitPolicy RESET_CONFIRM_POLICY = new RateLimitPolicy(10, Duration.ofMinutes(15));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RegistrationService registrationService;

	@MockitoBean
	private RegistrationWebMapper registrationWebMapper;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private AuthWebMapper authWebMapper;

	@MockitoBean
	private RefreshCookieFactory cookieFactory;

	@MockitoBean
	private CurrentActorProvider currentActorProvider;

	@MockitoBean
	private PasswordResetService passwordResetService;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private AuthRateLimiter authRateLimiter;

	@BeforeEach
	void setUp() {
		when(authRateLimiter.check(anyString(), any())).thenReturn(new RateLimitDecision(true, Duration.ZERO));
	}

	@Test
	void shouldAllowAnonymousPasswordResetRequestWithCsrf() throws Exception {

		mockMvc.perform(post(RESET_REQUEST_URL).servletPath(RESET_REQUEST_URL).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(RESET_REQUEST_JSON)).andExpect(status().isAccepted());

		verify(authRateLimiter).check(startsWith("auth:request:ip:"), eq(RESET_REQUEST_POLICY));

		verify(passwordResetService).requestPasswordReset("anna@example.com");
	}

	@Test
	void shouldAllowAnonymousPasswordResetConfirmWithCsrf() throws Exception {

		mockMvc.perform(post(RESET_CONFIRM_URL).servletPath(RESET_CONFIRM_URL).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(RESET_CONFIRM_JSON)).andExpect(status().isNoContent());

		verify(authRateLimiter).check(startsWith("auth:confirm:ip:"), eq(RESET_CONFIRM_POLICY));

		verify(passwordResetService).confirmPasswordReset("valid-reset-token", "NewStrongPassword123!");
	}

	@Test
	void shouldRejectPasswordResetRequestWithoutCsrf() throws Exception {

		mockMvc.perform(post(RESET_REQUEST_URL).servletPath(RESET_REQUEST_URL).contentType(MediaType.APPLICATION_JSON)
				.content(RESET_REQUEST_JSON)).andExpect(status().isForbidden());

		verifyNoInteractions(passwordResetService);
		verifyNoInteractions(authRateLimiter);
	}

	@Test
	void shouldRejectPasswordResetConfirmWithoutCsrf() throws Exception {

		mockMvc.perform(post(RESET_CONFIRM_URL).servletPath(RESET_CONFIRM_URL).contentType(MediaType.APPLICATION_JSON)
				.content(RESET_CONFIRM_JSON)).andExpect(status().isForbidden());

		verifyNoInteractions(passwordResetService);
		verifyNoInteractions(authRateLimiter);
	}

	@Test
	void shouldRejectLoginFromForbiddenOrigin() throws Exception {

		String json = """
				{
				    "email": "anna@example.com",
				    "password": "StrongPassword123!"
				}
				""";

		mockMvc.perform(post(LOGIN_URL).servletPath(LOGIN_URL).with(csrf()).header(HttpHeaders.ORIGIN, FORBIDDEN_ORIGIN)
				.contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isForbidden());

		verifyNoInteractions(authService);
		verifyNoInteractions(authRateLimiter);
	}

	@Test
	void shouldReturn429WhenPasswordResetRequestRateLimitExceeded() throws Exception {

		when(authRateLimiter.check(startsWith("auth:request:ip:"), eq(RESET_REQUEST_POLICY)))
				.thenReturn(new RateLimitDecision(false, Duration.ofSeconds(30)));

		mockMvc.perform(post(RESET_REQUEST_URL).servletPath(RESET_REQUEST_URL).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(RESET_REQUEST_JSON))
				.andExpect(status().isTooManyRequests()).andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));

		verify(authRateLimiter).check(startsWith("auth:request:ip:"), eq(RESET_REQUEST_POLICY));

		verifyNoInteractions(passwordResetService);
	}

	@Test
	void shouldReturn429WhenPasswordResetConfirmRateLimitExceeded() throws Exception {

		when(authRateLimiter.check(startsWith("auth:confirm:ip:"), eq(RESET_CONFIRM_POLICY)))
				.thenReturn(new RateLimitDecision(false, Duration.ofSeconds(60)));

		mockMvc.perform(post(RESET_CONFIRM_URL).servletPath(RESET_CONFIRM_URL).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(RESET_CONFIRM_JSON))
				.andExpect(status().isTooManyRequests()).andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));

		verify(authRateLimiter).check(startsWith("auth:confirm:ip:"), eq(RESET_CONFIRM_POLICY));

		verifyNoInteractions(passwordResetService);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestClockConfig {

		@Bean
		Clock clock() {
			return Clock.systemUTC();
		}
	}
}