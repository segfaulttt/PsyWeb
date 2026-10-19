
package com.psyweb.auth.web.controller;

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

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.psyweb.auth.exception.InvalidPasswordException;
import com.psyweb.auth.service.RegistrationService;
import com.psyweb.auth.web.mapper.RegistrationWebMapper;
import com.psyweb.client.domain.Client;
import com.psyweb.common.web.exception.GlobalExceptionHandler;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.user.exception.UserEmailAlreadyExistsException;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

	private static final String CLIENT_URL = "/api/auth/register/client";

	private static final String SPECIALIST_URL = "/api/auth/register/specialist";

	@Mock
	private RegistrationService registrationService;

	private MockMvc mockMvc;

	private LocalValidatorFactoryBean validator;

	@BeforeEach
	void setUp() {
		RegistrationWebMapper mapper = new RegistrationWebMapper();

		AuthController controller = new AuthController(registrationService, mapper);

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

		when(registrationService.registerSpecialist("kate@example.com", "StrongPassword123!", "Kate", "Jones"))
				.thenReturn(specialist);

		String json = """
				{
				  "email": "kate@example.com",
				  "password": "StrongPassword123!",
				  "firstName": "Kate",
				  "lastName": "Jones"
				}
				""";

		mockMvc.perform(post(SPECIALIST_URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.userId").value(20))
				.andExpect(jsonPath("$.role").value("SPECIALIST")).andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.approvalStatus").doesNotExist());

		verify(registrationService).registerSpecialist("kate@example.com", "StrongPassword123!", "Kate", "Jones");
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
}
