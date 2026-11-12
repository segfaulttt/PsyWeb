package com.psyweb.user.web.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Clock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.psyweb.client.web.dto.response.ClientProfileResponse;
import com.psyweb.common.web.exception.GlobalExceptionHandler;
import com.psyweb.specialist.domain.SpecialistStatus;
import com.psyweb.specialist.web.dto.response.SpecialistProfileResponse;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.exception.UserAccessForbiddenException;
import com.psyweb.user.exception.UserRoleMismatchException;
import com.psyweb.user.service.CurrentUserProfileService;
import com.psyweb.user.service.dto.CurrentUserProfile;
import com.psyweb.user.web.dto.response.ClientMeResponse;
import com.psyweb.user.web.dto.response.SpecialistMeResponse;
import com.psyweb.user.web.dto.response.UserAccountResponse;
import com.psyweb.user.web.mapper.MeWebMapper;
import com.psyweb.auth.authorization.CurrentActorProvider;
import com.psyweb.auth.service.PasswordService;
import com.psyweb.auth.web.cookie.RefreshCookieFactory;
import com.psyweb.client.domain.Client;
import com.psyweb.user.domain.User;
import com.psyweb.user.service.dto.CurrentClientProfile;
import static org.mockito.ArgumentMatchers.argThat;

import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.service.dto.UpdateCurrentProfileCommand;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.user.service.dto.CurrentSpecialistProfile;

import tools.jackson.databind.JsonNode;

@ExtendWith(MockitoExtension.class)
class MeControllerTest {

	private static final String ME_URL = "/api/me";
	private static final String ME_PROFILE_URL = "/api/me/profile";

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

	@BeforeEach
	void setUp() {
		MeController controller = new MeController(currentUserProfileService, meWebMapper, passwordService,
				currentActorProvider, refreshCookieFactory);

		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler(Clock.systemUTC())).build();
	}

	@Test
	void shouldReturnCurrentClientProfile() throws Exception {
		CurrentUserProfile profile = new CurrentClientProfile(mock(User.class), mock(Client.class));

		ClientMeResponse response = new ClientMeResponse(
				new UserAccountResponse(1L, "client@example.com", UserRole.CLIENT),
				new ClientProfileResponse(1L, "Anna", "Smith"));

		when(currentUserProfileService.getCurrentUserProfile()).thenReturn(profile);
		when(meWebMapper.toResponse(profile)).thenReturn(response);

		mockMvc.perform(get(ME_URL)).andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.account.id").value(1))
				.andExpect(jsonPath("$.account.email").value("client@example.com"))
				.andExpect(jsonPath("$.account.role").value("CLIENT")).andExpect(jsonPath("$.profile.id").value(1))
				.andExpect(jsonPath("$.profile.firstName").value("Anna"))
				.andExpect(jsonPath("$.profile.lastName").value("Smith"))
				.andExpect(jsonPath("$.profile.approvalStatus").doesNotExist())
				.andExpect(jsonPath("$.account.passwordHash").doesNotExist());

		verify(currentUserProfileService).getCurrentUserProfile();
		verify(meWebMapper).toResponse(profile);
	}

	@Test
	void shouldReturnCurrentSpecialistProfile() throws Exception {
		CurrentUserProfile profile = new CurrentClientProfile(mock(User.class), mock(Client.class));

		SpecialistMeResponse response = new SpecialistMeResponse(
				new UserAccountResponse(2L, "specialist@example.com", UserRole.SPECIALIST),
				new SpecialistProfileResponse(2L, "Kate", "Jones", SpecialistStatus.PENDING));

		when(currentUserProfileService.getCurrentUserProfile()).thenReturn(profile);
		when(meWebMapper.toResponse(profile)).thenReturn(response);

		mockMvc.perform(get(ME_URL)).andExpect(status().isOk()).andExpect(jsonPath("$.account.id").value(2))
				.andExpect(jsonPath("$.account.email").value("specialist@example.com"))
				.andExpect(jsonPath("$.account.role").value("SPECIALIST")).andExpect(jsonPath("$.profile.id").value(2))
				.andExpect(jsonPath("$.profile.firstName").value("Kate"))
				.andExpect(jsonPath("$.profile.lastName").value("Jones"))
				.andExpect(jsonPath("$.profile.approvalStatus").value("PENDING"));

		verify(currentUserProfileService).getCurrentUserProfile();
		verify(meWebMapper).toResponse(profile);
	}

	@Test
	void shouldDisableCachingOfProfileResponse() throws Exception {
		CurrentUserProfile profile = new CurrentClientProfile(mock(User.class), mock(Client.class));

		ClientMeResponse response = new ClientMeResponse(
				new UserAccountResponse(1L, "client@example.com", UserRole.CLIENT),
				new ClientProfileResponse(1L, "Anna", "Smith"));

		when(currentUserProfileService.getCurrentUserProfile()).thenReturn(profile);
		when(meWebMapper.toResponse(profile)).thenReturn(response);

		mockMvc.perform(get(ME_URL)).andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
	}

	@Test
	void shouldReturn401WhenAuthenticatedRoleDoesNotMatch() throws Exception {

		when(currentUserProfileService.getCurrentUserProfile())
				.thenThrow(new UserRoleMismatchException("Authenticated role does not match current user role"));

		mockMvc.perform(get(ME_URL)).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("USER_ROLE_MISMATCH"))
				.andExpect(jsonPath("$.message").value("Authenticated role does not match current user role"));

		verifyNoInteractions(meWebMapper);
	}

	@Test
	void shouldReturn403WhenUserAccountIsBlocked() throws Exception {

		when(currentUserProfileService.getCurrentUserProfile())
				.thenThrow(new UserAccessForbiddenException("User must have status 'ACTIVE'"));

		mockMvc.perform(get(ME_URL)).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("USER_ACCESS_FORBIDDEN"));

		verifyNoInteractions(meWebMapper);
	}

	@Test
	void shouldUpdateCurrentClientProfile() throws Exception {
		CurrentUserProfile profile = new CurrentClientProfile(mock(User.class), mock(Client.class));

		UpdateCurrentProfileCommand command = new UpdateCurrentProfileCommand("Anna", null);

		ClientMeResponse response = new ClientMeResponse(
				new UserAccountResponse(1L, "client@example.com", UserRole.CLIENT),
				new ClientProfileResponse(1L, "Anna", "Smith"));

		when(meWebMapper.toUpdateCommand(any(JsonNode.class))).thenReturn(command);
		when(currentUserProfileService.updateCurrentProfile(command)).thenReturn(profile);
		when(meWebMapper.toResponse(profile)).thenReturn(response);

		mockMvc.perform(patch(ME_PROFILE_URL).contentType(MediaType.APPLICATION_JSON).content("""
				{"firstName": "Anna"}
				""")).andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.account.id").value(1))
				.andExpect(jsonPath("$.account.email").value("client@example.com"))
				.andExpect(jsonPath("$.account.role").value("CLIENT"))
				.andExpect(jsonPath("$.profile.firstName").value("Anna"))
				.andExpect(jsonPath("$.profile.lastName").value("Smith"))
				.andExpect(jsonPath("$.account.passwordHash").doesNotExist());

		verify(meWebMapper).toUpdateCommand(
				argThat(json -> json.has("firstName") && "Anna".equals(json.get("firstName").asString())));
		verify(currentUserProfileService).updateCurrentProfile(command);
		verify(meWebMapper).toResponse(profile);
	}

	@Test
	void shouldUpdateCurrentSpecialistProfile() throws Exception {
		CurrentUserProfile profile = new CurrentSpecialistProfile(mock(User.class), mock(Specialist.class));

		UpdateCurrentProfileCommand command = new UpdateCurrentProfileCommand(null, "Jones");

		SpecialistMeResponse response = new SpecialistMeResponse(
				new UserAccountResponse(2L, "specialist@example.com", UserRole.SPECIALIST),
				new SpecialistProfileResponse(2L, "Kate", "Jones", SpecialistStatus.PENDING));

		when(meWebMapper.toUpdateCommand(any(JsonNode.class))).thenReturn(command);
		when(currentUserProfileService.updateCurrentProfile(command)).thenReturn(profile);
		when(meWebMapper.toResponse(profile)).thenReturn(response);

		mockMvc.perform(patch(ME_PROFILE_URL).contentType(MediaType.APPLICATION_JSON).content("""
				{"lastName": "Jones"}
				""")).andExpect(status().isOk()).andExpect(jsonPath("$.account.role").value("SPECIALIST"))
				.andExpect(jsonPath("$.profile.firstName").value("Kate"))
				.andExpect(jsonPath("$.profile.lastName").value("Jones"))
				.andExpect(jsonPath("$.profile.approvalStatus").value("PENDING"));

		verify(currentUserProfileService).updateCurrentProfile(command);
	}

	@Test
	void shouldDisableCachingOfUpdatedProfile() throws Exception {
		CurrentUserProfile profile = new CurrentClientProfile(mock(User.class), mock(Client.class));

		UpdateCurrentProfileCommand command = new UpdateCurrentProfileCommand("Anna", null);

		ClientMeResponse response = new ClientMeResponse(
				new UserAccountResponse(1L, "client@example.com", UserRole.CLIENT),
				new ClientProfileResponse(1L, "Anna", "Smith"));

		when(meWebMapper.toUpdateCommand(any(JsonNode.class))).thenReturn(command);
		when(currentUserProfileService.updateCurrentProfile(command)).thenReturn(profile);
		when(meWebMapper.toResponse(profile)).thenReturn(response);

		mockMvc.perform(patch(ME_PROFILE_URL).contentType(MediaType.APPLICATION_JSON).content("""
				{"firstName": "Anna"}
				""")).andExpect(status().isOk()).andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
	}

	@Test
	void shouldReturn400WhenProfileUpdateIsInvalid() throws Exception {
		when(meWebMapper.toUpdateCommand(any(JsonNode.class)))
				.thenThrow(new InvalidUserDataException("Unknown profile field: role"));

		mockMvc.perform(patch(ME_PROFILE_URL).contentType(MediaType.APPLICATION_JSON).content("""
				{"role": "ADMIN"}
				""")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Unknown profile field: role"));

		verifyNoInteractions(currentUserProfileService);
	}

	@Test
	void shouldReturn400WhenRequestBodyIsMissing() throws Exception {
		mockMvc.perform(patch(ME_PROFILE_URL).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(currentUserProfileService, meWebMapper);
	}

	@Test
	void shouldReturn403WhenUpdatingBlockedUser() throws Exception {
		UpdateCurrentProfileCommand command = new UpdateCurrentProfileCommand("Anna", null);

		when(meWebMapper.toUpdateCommand(any(JsonNode.class))).thenReturn(command);
		when(currentUserProfileService.updateCurrentProfile(command))
				.thenThrow(new UserAccessForbiddenException("User must have status 'ACTIVE'"));

		mockMvc.perform(patch(ME_PROFILE_URL).contentType(MediaType.APPLICATION_JSON).content("""
				{"firstName": "Anna"}
				""")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("USER_ACCESS_FORBIDDEN"));

		verify(meWebMapper, never()).toResponse(any());
	}

	@Test
	void shouldReturn401WhenAuthenticatedRoleDoesNotMatchOnUpdate() throws Exception {

		UpdateCurrentProfileCommand command = new UpdateCurrentProfileCommand("Anna", null);

		when(meWebMapper.toUpdateCommand(any(JsonNode.class))).thenReturn(command);
		when(currentUserProfileService.updateCurrentProfile(command))
				.thenThrow(new UserRoleMismatchException("Authenticated role does not match current user role"));

		mockMvc.perform(patch(ME_PROFILE_URL).contentType(MediaType.APPLICATION_JSON).content("""
				{"firstName": "Anna"}
				""")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("USER_ROLE_MISMATCH"));

		verify(meWebMapper, never()).toResponse(any());
	}
}