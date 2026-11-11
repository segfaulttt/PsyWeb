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
import com.psyweb.client.domain.Client;
import com.psyweb.user.domain.User;
import com.psyweb.user.service.dto.CurrentClientProfile;

@ExtendWith(MockitoExtension.class)
class MeControllerTest {

	private static final String ME_URL = "/api/me";

	@Mock
	private CurrentUserProfileService currentUserProfileService;

	@Mock
	private MeWebMapper meWebMapper;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		MeController controller = new MeController(currentUserProfileService, meWebMapper);

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
}