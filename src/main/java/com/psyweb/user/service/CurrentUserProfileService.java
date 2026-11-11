package com.psyweb.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psyweb.auth.authorization.CurrentActor;
import com.psyweb.auth.authorization.CurrentActorProvider;
import com.psyweb.client.domain.Client;
import com.psyweb.client.service.ClientService;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.service.SpecialistService;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.exception.UserRoleMismatchException;
import com.psyweb.user.service.dto.CurrentClientProfile;
import com.psyweb.user.service.dto.CurrentSpecialistProfile;
import com.psyweb.user.service.dto.CurrentUserProfile;
import com.psyweb.user.service.dto.UpdateCurrentProfileCommand;

@Service
public class CurrentUserProfileService {
	private final UserService userService;
	private final SpecialistService specialistService;
	private final ClientService clientService;
	private final CurrentActorProvider currentActorProvider;

	public CurrentUserProfileService(UserService userService, SpecialistService specialistService,
			ClientService clientService, CurrentActorProvider actorProvider) {
		this.userService = userService;
		this.specialistService = specialistService;
		this.clientService = clientService;
		this.currentActorProvider = actorProvider;
	}

	@Transactional(readOnly = true)
	public CurrentUserProfile getCurrentUserProfile() {
		CurrentActor actor = currentActorProvider.requireCurrentActor();

		User user = userService.getActiveUser(actor.userId());
		if (user.getRole() != actor.role()) {
			throw new UserRoleMismatchException("Authenticated role does not match current user role");
		}
		if (user.getRole() == UserRole.CLIENT) {
			Client client = clientService.getClient(user.getId());
			return new CurrentClientProfile(user, client);
		} else if (user.getRole() == UserRole.SPECIALIST) {
			Specialist specialist = specialistService.getSpecialist(user.getId());
			return new CurrentSpecialistProfile(user, specialist);
		} else {
			throw new IllegalStateException("Incorrect role");
		}
	}

	@Transactional
	public CurrentUserProfile updateCurrentProfile(UpdateCurrentProfileCommand command) {
		if (command == null) {
			throw new InvalidUserDataException("Profile update command cannot be null");
		}

		if (command.firstName() == null && command.lastName() == null) {
			throw new InvalidUserDataException("At least one profile field must be provided");
		}

		CurrentActor actor = currentActorProvider.requireCurrentActor();

		User user = userService.getActiveUserForUpdate(actor.userId());

		if (user.getRole() != actor.role()) {
			throw new UserRoleMismatchException("Authenticated role does not match current user role");
		}

		return switch (user.getRole()) {
		case CLIENT -> new CurrentClientProfile(user,
				clientService.updateProfile(user.getId(), command.firstName(), command.lastName()));

		case SPECIALIST -> new CurrentSpecialistProfile(user,
				specialistService.updateProfile(user.getId(), command.firstName(), command.lastName()));

		default -> throw new IllegalStateException("Unsupported user role for profile update");
		};
	}
}
