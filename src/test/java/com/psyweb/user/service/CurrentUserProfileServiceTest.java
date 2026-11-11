package com.psyweb.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.psyweb.auth.authorization.CurrentActor;
import com.psyweb.auth.authorization.CurrentActorProvider;
import com.psyweb.client.domain.Client;
import com.psyweb.client.service.ClientService;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.service.SpecialistService;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.exception.UserAccessForbiddenException;
import com.psyweb.user.exception.UserRoleMismatchException;
import com.psyweb.user.service.dto.CurrentClientProfile;
import com.psyweb.user.service.dto.CurrentSpecialistProfile;
import com.psyweb.user.service.dto.CurrentUserProfile;
import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.service.dto.UpdateCurrentProfileCommand;

@ExtendWith(MockitoExtension.class)
class CurrentUserProfileServiceTest {

	private static final Long USER_ID = 1L;

	@Mock
	private UserService userService;

	@Mock
	private ClientService clientService;

	@Mock
	private SpecialistService specialistService;

	@Mock
	private CurrentActorProvider currentActorProvider;

	@InjectMocks
	private CurrentUserProfileService service;

	@Test
	void shouldReturnCurrentClientProfile() {
		CurrentActor actor = new CurrentActor(USER_ID, UserRole.CLIENT);
		User user = mock(User.class);
		Client client = mock(Client.class);

		when(currentActorProvider.requireCurrentActor()).thenReturn(actor);
		when(userService.getActiveUser(USER_ID)).thenReturn(user);
		when(user.getRole()).thenReturn(UserRole.CLIENT);
		when(user.getId()).thenReturn(USER_ID);
		when(clientService.getClient(USER_ID)).thenReturn(client);

		CurrentUserProfile result = service.getCurrentUserProfile();

		assertInstanceOf(CurrentClientProfile.class, result);

		CurrentClientProfile profile = (CurrentClientProfile) result;

		assertSame(user, profile.user());
		assertSame(client, profile.client());

		verify(currentActorProvider).requireCurrentActor();
		verify(userService).getActiveUser(USER_ID);
		verify(clientService).getClient(USER_ID);
		verifyNoInteractions(specialistService);
	}

	@Test
	void shouldReturnCurrentSpecialistProfile() {
		CurrentActor actor = new CurrentActor(USER_ID, UserRole.SPECIALIST);
		User user = mock(User.class);
		Specialist specialist = mock(Specialist.class);

		when(currentActorProvider.requireCurrentActor()).thenReturn(actor);
		when(userService.getActiveUser(USER_ID)).thenReturn(user);
		when(user.getRole()).thenReturn(UserRole.SPECIALIST);
		when(user.getId()).thenReturn(USER_ID);
		when(specialistService.getSpecialist(USER_ID)).thenReturn(specialist);

		CurrentUserProfile result = service.getCurrentUserProfile();

		assertInstanceOf(CurrentSpecialistProfile.class, result);

		CurrentSpecialistProfile profile = (CurrentSpecialistProfile) result;

		assertSame(user, profile.user());
		assertSame(specialist, profile.specialist());

		verify(specialistService).getSpecialist(USER_ID);
		verify(specialistService, never()).getEligibleSpecialist(anyLong());
		verifyNoInteractions(clientService);
	}

	@Test
	void shouldRejectUserWhenAuthenticatedRoleDoesNotMatch() {
		CurrentActor actor = new CurrentActor(USER_ID, UserRole.CLIENT);
		User user = mock(User.class);

		when(currentActorProvider.requireCurrentActor()).thenReturn(actor);
		when(userService.getActiveUser(USER_ID)).thenReturn(user);
		when(user.getRole()).thenReturn(UserRole.SPECIALIST);

		UserRoleMismatchException exception = assertThrows(UserRoleMismatchException.class,
				() -> service.getCurrentUserProfile());

		assertEquals("USER_ROLE_MISMATCH", exception.code());

		verifyNoInteractions(clientService, specialistService);
	}

	@Test
	void shouldRejectBlockedUserBeforeLoadingProfile() {
		CurrentActor actor = new CurrentActor(USER_ID, UserRole.CLIENT);

		when(currentActorProvider.requireCurrentActor()).thenReturn(actor);
		when(userService.getActiveUser(USER_ID))
				.thenThrow(new UserAccessForbiddenException("User must have status 'ACTIVE'"));

		UserAccessForbiddenException exception = assertThrows(UserAccessForbiddenException.class,
				() -> service.getCurrentUserProfile());

		assertEquals("USER_ACCESS_FORBIDDEN", exception.code());

		verify(userService).getActiveUser(USER_ID);
		verifyNoInteractions(clientService, specialistService);
	}

	@Test
	void shouldRejectUnsupportedUserRole() {
		CurrentActor actor = new CurrentActor(USER_ID, UserRole.ADMIN);
		User user = mock(User.class);

		when(currentActorProvider.requireCurrentActor()).thenReturn(actor);
		when(userService.getActiveUser(USER_ID)).thenReturn(user);
		when(user.getRole()).thenReturn(UserRole.ADMIN);

		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> service.getCurrentUserProfile());

		assertEquals("Incorrect role", exception.getMessage());

		verifyNoInteractions(clientService, specialistService);
	}

	@Test
	void shouldUpdateCurrentClientFirstNameOnly() {
		CurrentActor actor = new CurrentActor(USER_ID, UserRole.CLIENT);
		User user = mock(User.class);
		Client client = mock(Client.class);
		UpdateCurrentProfileCommand command = new UpdateCurrentProfileCommand("Anna", null);

		when(currentActorProvider.requireCurrentActor()).thenReturn(actor);
		when(userService.getActiveUserForUpdate(USER_ID)).thenReturn(user);
		when(user.getRole()).thenReturn(UserRole.CLIENT);
		when(user.getId()).thenReturn(USER_ID);
		when(clientService.updateProfile(USER_ID, "Anna", null)).thenReturn(client);

		CurrentUserProfile result = service.updateCurrentProfile(command);

		assertInstanceOf(CurrentClientProfile.class, result);

		CurrentClientProfile profile = (CurrentClientProfile) result;
		assertSame(user, profile.user());
		assertSame(client, profile.client());

		verify(clientService).updateProfile(USER_ID, "Anna", null);
		verify(userService).getActiveUserForUpdate(USER_ID);
		verify(userService, never()).getActiveUser(anyLong());
		verifyNoInteractions(specialistService);
	}

	@Test
	void shouldUpdateBothClientProfileFields() {
		CurrentActor actor = new CurrentActor(USER_ID, UserRole.CLIENT);
		User user = mock(User.class);
		Client client = mock(Client.class);
		UpdateCurrentProfileCommand command = new UpdateCurrentProfileCommand("Anna", "Smith");

		when(currentActorProvider.requireCurrentActor()).thenReturn(actor);
		when(userService.getActiveUserForUpdate(USER_ID)).thenReturn(user);
		when(user.getRole()).thenReturn(UserRole.CLIENT);
		when(user.getId()).thenReturn(USER_ID);
		when(clientService.updateProfile(USER_ID, "Anna", "Smith")).thenReturn(client);

		CurrentUserProfile result = service.updateCurrentProfile(command);

		assertInstanceOf(CurrentClientProfile.class, result);

		verify(clientService).updateProfile(USER_ID, "Anna", "Smith");
		verifyNoInteractions(specialistService);
	}

	@Test
	void shouldUpdateCurrentSpecialistProfile() {
		CurrentActor actor = new CurrentActor(USER_ID, UserRole.SPECIALIST);
		User user = mock(User.class);
		Specialist specialist = mock(Specialist.class);
		UpdateCurrentProfileCommand command = new UpdateCurrentProfileCommand(null, "Jones");

		when(currentActorProvider.requireCurrentActor()).thenReturn(actor);
		when(userService.getActiveUserForUpdate(USER_ID)).thenReturn(user);
		when(user.getRole()).thenReturn(UserRole.SPECIALIST);
		when(user.getId()).thenReturn(USER_ID);
		when(specialistService.updateProfile(USER_ID, null, "Jones")).thenReturn(specialist);

		CurrentUserProfile result = service.updateCurrentProfile(command);

		assertInstanceOf(CurrentSpecialistProfile.class, result);

		CurrentSpecialistProfile profile = (CurrentSpecialistProfile) result;

		assertSame(user, profile.user());
		assertSame(specialist, profile.specialist());

		verify(specialistService).updateProfile(USER_ID, null, "Jones");
		verifyNoInteractions(clientService);
	}

	@Test
	void shouldRejectNullUpdateCommand() {
		InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
				() -> service.updateCurrentProfile(null));

		assertEquals("USER_INVALID_DATA", exception.code());
		assertEquals("Profile update command cannot be null", exception.getMessage());
		verifyNoInteractions(currentActorProvider, userService, clientService, specialistService);
	}

	@Test
	void shouldRejectUpdateCommandWithoutFields() {
		UpdateCurrentProfileCommand command = new UpdateCurrentProfileCommand(null, null);

		InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
				() -> service.updateCurrentProfile(command));

		assertEquals("USER_INVALID_DATA", exception.code());
		assertEquals("At least one profile field must be provided", exception.getMessage());
		verifyNoInteractions(currentActorProvider, userService, clientService, specialistService);
	}

	@Test
	void shouldRejectProfileUpdateWhenAuthenticatedRoleDoesNotMatch() {
		CurrentActor actor = new CurrentActor(USER_ID, UserRole.CLIENT);
		User user = mock(User.class);
		UpdateCurrentProfileCommand command = new UpdateCurrentProfileCommand("Anna", null);

		when(currentActorProvider.requireCurrentActor()).thenReturn(actor);
		when(userService.getActiveUserForUpdate(USER_ID)).thenReturn(user);
		when(user.getRole()).thenReturn(UserRole.SPECIALIST);

		UserRoleMismatchException exception = assertThrows(UserRoleMismatchException.class,
				() -> service.updateCurrentProfile(command));

		assertEquals("USER_ROLE_MISMATCH", exception.code());

		verifyNoInteractions(clientService, specialistService);
	}

	@Test
	void shouldRejectProfileUpdateWhenUserIsBlocked() {
		CurrentActor actor = new CurrentActor(USER_ID, UserRole.CLIENT);
		UpdateCurrentProfileCommand command = new UpdateCurrentProfileCommand("Anna", null);

		when(currentActorProvider.requireCurrentActor()).thenReturn(actor);
		when(userService.getActiveUserForUpdate(USER_ID))
				.thenThrow(new UserAccessForbiddenException("User must have status 'ACTIVE'"));

		UserAccessForbiddenException exception = assertThrows(UserAccessForbiddenException.class,
				() -> service.updateCurrentProfile(command));

		assertEquals("USER_ACCESS_FORBIDDEN", exception.code());

		verify(userService).getActiveUserForUpdate(USER_ID);
		verifyNoInteractions(clientService, specialistService);
	}

	@Test
	void shouldRejectProfileUpdateForUnsupportedRole() {
		CurrentActor actor = new CurrentActor(USER_ID, UserRole.ADMIN);
		User user = mock(User.class);
		UpdateCurrentProfileCommand command = new UpdateCurrentProfileCommand("Anna", null);

		when(currentActorProvider.requireCurrentActor()).thenReturn(actor);
		when(userService.getActiveUserForUpdate(USER_ID)).thenReturn(user);
		when(user.getRole()).thenReturn(UserRole.ADMIN);

		assertThrows(IllegalStateException.class, () -> service.updateCurrentProfile(command));

		verifyNoInteractions(clientService, specialistService);
	}
}