package com.psyweb.user.web.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Clock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.psyweb.auth.authorization.CurrentActor;
import com.psyweb.auth.authorization.CurrentActorProvider;
import com.psyweb.auth.exception.InvalidCurrentPasswordException;
import com.psyweb.auth.exception.InvalidPasswordException;
import com.psyweb.auth.service.PasswordService;
import com.psyweb.auth.web.cookie.RefreshCookieFactory;
import com.psyweb.common.web.exception.GlobalExceptionHandler;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.service.CurrentUserProfileService;
import com.psyweb.user.web.mapper.MeWebMapper;

@ExtendWith(MockitoExtension.class)
class MePasswordControllerTest {

	private static final String URL = "/api/me/password";
	private static final Long USER_ID = 42L;
	private static final String CURRENT_PASSWORD = "CurrentPassword123!";
	private static final String NEW_PASSWORD = "NewStrongPassword123!";

	@Mock
	private CurrentUserProfileService currentUserProfileService;

	@Mock
	private MeWebMapper meWebMapper;

	@Mock
	private PasswordService passwordService;

	@Mock
	private CurrentActorProvider currentActorProvider;

	@Mock
	private RefreshCookieFactory refreshCookieFactory;

	private MockMvc mockMvc;
	private LocalValidatorFactoryBean validator;

	@BeforeEach
	void setUp() {
		MeController controller = new MeController(currentUserProfileService, meWebMapper, passwordService,
				currentActorProvider, refreshCookieFactory);

		validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler(Clock.systemUTC())).setValidator(validator).build();
	}

	@AfterEach
	void tearDown() {
		validator.close();
	}

	private String validRequest() {
		return """
				{
				    "currentPassword": "CurrentPassword123!",
				    "newPassword": "NewStrongPassword123!"
				}
				""";
	}

	private void mockCurrentActor() {
		when(currentActorProvider.requireCurrentActor()).thenReturn(new CurrentActor(USER_ID, UserRole.CLIENT));
	}

	@Test
	void shouldChangePasswordAndReturn204() throws Exception {
		mockCurrentActor();

		ResponseCookie cookie = ResponseCookie.from("__Host-refreshToken", "").httpOnly(true).secure(true)
				.sameSite("Strict").path("/").maxAge(0).build();

		when(refreshCookieFactory.clear()).thenReturn(cookie);

		mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON).content(validRequest()))
				.andExpect(status().isNoContent()).andExpect(content().string(""))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
				.andExpect(header().string(HttpHeaders.SET_COOKIE,
						org.hamcrest.Matchers.containsString("__Host-refreshToken=")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Secure")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE,
						org.hamcrest.Matchers.containsString("SameSite=Strict")));

		verify(passwordService).changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD);
		verify(refreshCookieFactory).clear();
		verifyNoInteractions(currentUserProfileService, meWebMapper);
	}

	@Test
	void shouldUseAuthenticatedUserIdInsteadOfRequestUserId() throws Exception {
		mockCurrentActor();
		when(refreshCookieFactory.clear()).thenReturn(ResponseCookie.from("__Host-refreshToken", "").httpOnly(true)
				.secure(true).sameSite("Strict").path("/").maxAge(0).build());

		mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON).content("""
				{
				    "userId": 999,
				    "currentPassword": "CurrentPassword123!",
				    "newPassword": "NewStrongPassword123!"
				}
				""")).andExpect(status().isNoContent());

		verify(passwordService).changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD);

		verify(passwordService, never()).changePassword(eq(999L), anyString(), anyString());
	}

	@Test
	void shouldRejectBlankCurrentPassword() throws Exception {
		mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON).content("""
				{
				    "currentPassword": " ",
				    "newPassword": "NewStrongPassword123!"
				}
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.currentPassword").isArray());

		verifyNoInteractions(currentActorProvider, passwordService, refreshCookieFactory);
	}

	@Test
	void shouldRejectBlankNewPassword() throws Exception {
		mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON).content("""
				{
				    "currentPassword": "CurrentPassword123!",
				    "newPassword": ""
				}
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.newPassword").isArray());

		verifyNoInteractions(currentActorProvider, passwordService, refreshCookieFactory);
	}

	@Test
	void shouldRejectMissingRequestBody() throws Exception {
		mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());

		verifyNoInteractions(currentActorProvider, passwordService, refreshCookieFactory);
	}

	@Test
	void shouldReturn400ForIncorrectCurrentPassword() throws Exception {

		mockCurrentActor();

		doThrow(new InvalidCurrentPasswordException("Incorrect current password")).when(passwordService)
				.changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD);

		mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON).content(validRequest()))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"))
				.andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());

		verifyNoInteractions(refreshCookieFactory);
	}

	@Test
	void shouldReturn400WhenNewPasswordViolatesPolicy() throws Exception {

		mockCurrentActor();

		doThrow(new InvalidPasswordException("Password does not meet requirements")).when(passwordService)
				.changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD);

		mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON).content(validRequest()))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PASSWORD_INVALID"));

		verifyNoInteractions(refreshCookieFactory);
	}

	@Test
	void shouldNotClearCookieWhenPasswordChangeFails() throws Exception {

		mockCurrentActor();

		doThrow(new IllegalStateException("Database failure")).when(passwordService).changePassword(USER_ID,
				CURRENT_PASSWORD, NEW_PASSWORD);

		mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON).content(validRequest()))
				.andExpect(status().isInternalServerError()).andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
				.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));

		verifyNoInteractions(refreshCookieFactory);
	}
}