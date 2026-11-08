
package com.psyweb.auth.web.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.psyweb.auth.exception.InvalidPasswordResetTokenException;
import com.psyweb.auth.authorization.CurrentActorProvider;
import com.psyweb.auth.exception.InvalidPasswordException;
import com.psyweb.auth.service.AuthService;
import com.psyweb.auth.service.PasswordResetService;
import com.psyweb.auth.service.RegistrationService;
import com.psyweb.auth.web.cookie.RefreshCookieFactory;
import com.psyweb.auth.web.mapper.AuthWebMapper;
import com.psyweb.auth.web.mapper.RegistrationWebMapper;
import com.psyweb.client.domain.Client;
import com.psyweb.common.web.exception.GlobalExceptionHandler;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.user.exception.UserEmailAlreadyExistsException;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

	private static final String CLIENT_URL = "/api/auth/register/client";

	private static final String SPECIALIST_URL = "/api/auth/register/specialist";

	private static final String PASSWORD_RESET_REQUEST_URL = "/api/auth/password-reset/request";

	private static final String PASSWORD_RESET_CONFIRM_URL = "/api/auth/password-reset/confirm";

	@Mock
	private RegistrationService registrationService;

	@Mock
	private AuthService authService;

	@Mock
	private AuthWebMapper authWebMapper;

	@Mock
	private RefreshCookieFactory cookieFactory;

	@Mock
	private CurrentActorProvider currentActorProvider;

	@Mock
	private PasswordResetService passwordResetService;

	private MockMvc mockMvc;

	private LocalValidatorFactoryBean validator;

	@BeforeEach
	void setUp() {
		RegistrationWebMapper mapper = new RegistrationWebMapper();

		AuthController controller = new AuthController(registrationService, mapper, authService, authWebMapper,
				cookieFactory, currentActorProvider, passwordResetService);

		validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler(Clock.systemUTC())).setValidator(validator).build();
	}

	@AfterEach
	void tearDown() {
		validator.close();
	}

	@Test
	void shouldRegisterClientAndReturn201() throws Exception {
		Client client = mock(Client.class);
		when(client.getId()).thenReturn(10L);

		when(registrationService.registerClient("anna@example.com", "StrongPassword123!", "Anna", "Smith"))
				.thenReturn(client);

		String json = """
				{
				  "email": "anna@example.com",
				  "password": "StrongPassword123!",
				  "firstName": "Anna",
				  "lastName": "Smith"
				}
				""";

		mockMvc.perform(post(CLIENT_URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.userId").value(10)).andExpect(jsonPath("$.role").value("CLIENT"))
				.andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());

		verify(registrationService).registerClient("anna@example.com", "StrongPassword123!", "Anna", "Smith");
	}

	@Test
	void shouldRegisterSpecialistAndReturn201() throws Exception {

		Specialist specialist = mock(Specialist.class);
		when(specialist.getId()).thenReturn(20L);

		when(registrationService.registerSpecialist("kate@example.com", "StrongPassword123!", "Kate", "Jones",
				ZoneId.of("Europe/Amsterdam"))).thenReturn(specialist);

		String json = """
				{
				  "email": "kate@example.com",
				  "password": "StrongPassword123!",
				  "firstName": "Kate",
				  "lastName": "Jones",
				  "timeZone": "Europe/Amsterdam"
				}
				""";

		mockMvc.perform(post(SPECIALIST_URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.userId").value(20))
				.andExpect(jsonPath("$.role").value("SPECIALIST")).andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.approvalStatus").doesNotExist());

		verify(registrationService).registerSpecialist("kate@example.com", "StrongPassword123!", "Kate", "Jones",
				ZoneId.of("Europe/Amsterdam"));
	}

	@Test
	void shouldReturn400WhenClientEmailIsInvalid() throws Exception {

		String json = """
				{
				  "email": "not-an-email",
				  "password": "StrongPassword123!",
				  "firstName": "Anna",
				  "lastName": "Smith"
				}
				""";

		mockMvc.perform(post(CLIENT_URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.email").isArray());

		verifyNoInteractions(registrationService);
	}

	@Test
	void shouldReturn400WhenSpecialistNameIsBlank() throws Exception {

		String json = """
				{
				  "email": "kate@example.com",
				  "password": "StrongPassword123!",
				  "firstName": " ",
				  "lastName": "Jones"
				}
				""";

		mockMvc.perform(post(SPECIALIST_URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.firstName").isArray());

		verifyNoInteractions(registrationService);
	}

	@Test
	void shouldReturn409WhenEmailAlreadyExists() throws Exception {

		when(registrationService.registerClient("anna@example.com", "StrongPassword123!", "Anna", "Smith"))
				.thenThrow(new UserEmailAlreadyExistsException("Email already exists"));

		String json = """
				{
				  "email": "anna@example.com",
				  "password": "StrongPassword123!",
				  "firstName": "Anna",
				  "lastName": "Smith"
				}
				""";

		mockMvc.perform(post(CLIENT_URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("USER_EMAIL_ALREADY_EXISTS"))
				.andExpect(jsonPath("$.path").value(CLIENT_URL));
	}

	@Test
	void shouldReturn400WhenPasswordPolicyFails() throws Exception {

		when(registrationService.registerClient("anna@example.com", "weak", "Anna", "Smith"))
				.thenThrow(new InvalidPasswordException("Password does not meet requirements"));

		String json = """
				{
				  "email": "anna@example.com",
				  "password": "weak",
				  "firstName": "Anna",
				  "lastName": "Smith"
				}
				""";

		mockMvc.perform(post(CLIENT_URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PASSWORD_INVALID"));
	}

	@Test
	void shouldReturn400ForMalformedJson() throws Exception {

		String invalidJson = """
				{
				  "email": "anna@example.com",
				  "password":
				""";

		mockMvc.perform(post(CLIENT_URL).contentType(MediaType.APPLICATION_JSON).content(invalidJson))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"))
				.andExpect(jsonPath("$.message").value("Malformed request body"));

		verifyNoInteractions(registrationService);
	}

	@Test
	void shouldNotAllowRoleOverrideThroughJson() throws Exception {

		Client client = mock(Client.class);
		when(client.getId()).thenReturn(30L);

		when(registrationService.registerClient("anna@example.com", "StrongPassword123!", "Anna", "Smith"))
				.thenReturn(client);

		String json = """
				{
				  "email": "anna@example.com",
				  "password": "StrongPassword123!",
				  "firstName": "Anna",
				  "lastName": "Smith",
				  "role": "ADMIN",
				  "approvalStatus": "APPROVED"
				}
				""";

		mockMvc.perform(post(CLIENT_URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("CLIENT"))
				.andExpect(jsonPath("$.approvalStatus").doesNotExist());

		verify(registrationService).registerClient("anna@example.com", "StrongPassword123!", "Anna", "Smith");
	}

	@Test
	void shouldReturn400WhenSpecialistTimeZoneMissing() throws Exception {
		String json = """
				{
				  "email": "kate@example.com",
				  "password": "StrongPassword123!",
				  "firstName": "Kate",
				  "lastName": "Jones"
				}
				""";

		mockMvc.perform(post(SPECIALIST_URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.timeZone").isArray());

		verifyNoInteractions(registrationService);
	}

	@Test
	void shouldReturn202WhenPasswordResetRequested() throws Exception {
		String json = """
				{
				  "email": "anna@example.com"
				}
				""";

		mockMvc.perform(post(PASSWORD_RESET_REQUEST_URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isAccepted()).andExpect(content().string(""));

		verify(passwordResetService).requestPasswordReset("anna@example.com");
	}

	@Test
	void shouldReturn400WhenPasswordResetEmailIsInvalid() throws Exception {
		String json = """
				{
				  "email": "invalid-email"
				}
				""";

		mockMvc.perform(post(PASSWORD_RESET_REQUEST_URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.email").isArray());

		verifyNoInteractions(passwordResetService);
	}

	@Test
	void shouldReturn400WhenPasswordResetEmailIsBlank() throws Exception {
		String json = """
				{
				  "email": " "
				}
				""";

		mockMvc.perform(post(PASSWORD_RESET_REQUEST_URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		verifyNoInteractions(passwordResetService);
	}

	@Test
	void shouldReturn204WhenPasswordResetConfirmed() throws Exception {
		String json = """
				{
				  "token": "valid-reset-token",
				  "newPassword": "NewStrongPassword123!"
				}
				""";

		mockMvc.perform(post(PASSWORD_RESET_CONFIRM_URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isNoContent()).andExpect(content().string(""));

		verify(passwordResetService).confirmPasswordReset("valid-reset-token", "NewStrongPassword123!");
	}

	@Test
	void shouldReturn400WhenPasswordResetTokenIsBlank() throws Exception {
		String json = """
				{
				  "token": " ",
				  "newPassword": "NewStrongPassword123!"
				}
				""";

		mockMvc.perform(post(PASSWORD_RESET_CONFIRM_URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.token").isArray());

		verifyNoInteractions(passwordResetService);
	}

	@Test
	void shouldReturn400WhenNewPasswordIsBlank() throws Exception {
		String json = """
				{
				  "token": "valid-reset-token",
				  "newPassword": " "
				}
				""";

		mockMvc.perform(post(PASSWORD_RESET_CONFIRM_URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.newPassword").isArray());

		verifyNoInteractions(passwordResetService);
	}

	@Test
	void shouldReturn400WhenPasswordResetTokenIsInvalid() throws Exception {
		String json = """
				{
				  "token": "invalid-reset-token",
				  "newPassword": "NewStrongPassword123!"
				}
				""";

		doThrow(new InvalidPasswordResetTokenException()).when(passwordResetService)
				.confirmPasswordReset("invalid-reset-token", "NewStrongPassword123!");

		mockMvc.perform(post(PASSWORD_RESET_CONFIRM_URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PASSWORD_RESET_TOKEN"))
				.andExpect(jsonPath("$.message").value("Invalid or expired password reset token"));

		verify(passwordResetService).confirmPasswordReset("invalid-reset-token", "NewStrongPassword123!");
	}
}
