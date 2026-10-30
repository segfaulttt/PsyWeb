
package com.psyweb.auth.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.psyweb.auth.exception.AuthorizationForbiddenException;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

class AuthorizationPolicyTest {

	private final AuthorizationPolicy policy = new AuthorizationPolicy();

	private User actor(Long id, UserRole role, UserStatus status) {
		User actor = mock(User.class);
		when(actor.getId()).thenReturn(id);
		when(actor.getRole()).thenReturn(role);
		when(actor.getStatus()).thenReturn(status);
		return actor;
	}

	private Specialist specialist(Long id) {
		Specialist specialist = mock(Specialist.class);
		when(specialist.getId()).thenReturn(id);
		return specialist;
	}

	private Specialist realSpecialist(User user) {
		return new Specialist(user, "Anna", "Ivanova", Duration.ZERO, Duration.ZERO);
	}

	// Active client

	@Test
	void shouldAllowActiveClient() {
		User client = actor(1L, UserRole.CLIENT, UserStatus.ACTIVE);

		assertDoesNotThrow(() -> policy.requireActiveClient(client));
	}

	@Test
	void shouldRejectNullClientActor() {
		assertThrows(IllegalArgumentException.class, () -> policy.requireActiveClient(null));
	}

	@Test
	void shouldRejectBlockedClient() {
		User client = actor(1L, UserRole.CLIENT, UserStatus.BLOCKED);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireActiveClient(client));
	}

	@ParameterizedTest
	@EnumSource(value = UserRole.class, names = { "SPECIALIST", "ADMIN" })
	void shouldRejectNonClientRole(UserRole role) {
		User user = actor(1L, role, UserStatus.ACTIVE);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireActiveClient(user));
	}

	// Active admin

	@Test
	void shouldAllowActiveAdmin() {
		User admin = actor(2L, UserRole.ADMIN, UserStatus.ACTIVE);

		assertDoesNotThrow(() -> policy.requireActiveAdmin(admin));
	}

	@Test
	void shouldRejectNullAdminActor() {
		assertThrows(IllegalArgumentException.class, () -> policy.requireActiveAdmin(null));
	}

	@Test
	void shouldRejectBlockedAdmin() {
		User admin = actor(2L, UserRole.ADMIN, UserStatus.BLOCKED);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireActiveAdmin(admin));
	}

	@ParameterizedTest
	@EnumSource(value = UserRole.class, names = { "CLIENT", "SPECIALIST" })
	void shouldRejectNonAdminRole(UserRole role) {
		User user = actor(2L, role, UserStatus.ACTIVE);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireActiveAdmin(user));
	}

	// Active specialist

	@Test
	void shouldAllowActiveSpecialist() {
		User user = actor(3L, UserRole.SPECIALIST, UserStatus.ACTIVE);

		assertDoesNotThrow(() -> policy.requireActiveSpecialist(user));
	}

	@Test
	void shouldRejectNullSpecialistActor() {
		assertThrows(IllegalArgumentException.class, () -> policy.requireActiveSpecialist(null));
	}

	@Test
	void shouldRejectBlockedSpecialistActor() {
		User user = actor(3L, UserRole.SPECIALIST, UserStatus.BLOCKED);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireActiveSpecialist(user));
	}

	@ParameterizedTest
	@EnumSource(value = UserRole.class, names = { "CLIENT", "ADMIN" })
	void shouldRejectNonSpecialistRole(UserRole role) {
		User user = actor(3L, role, UserStatus.ACTIVE);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireActiveSpecialist(user));
	}

	// Specialist cabinet access

	@Test
	void shouldAllowOwnSpecialistCabinet() {
		User user = actor(1000L, UserRole.SPECIALIST, UserStatus.ACTIVE);
		Specialist profile = specialist(1000L);

		assertDoesNotThrow(() -> policy.requireSpecialistCabinetAccess(user, profile));
	}

	@Test
	void shouldRejectForeignSpecialistCabinet() {
		User user = actor(1000L, UserRole.SPECIALIST, UserStatus.ACTIVE);
		Specialist profile = specialist(2000L);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireSpecialistCabinetAccess(user, profile));
	}

	@Test
	void shouldRejectCabinetWithMissingActorId() {
		User user = actor(null, UserRole.SPECIALIST, UserStatus.ACTIVE);
		Specialist profile = specialist(1000L);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireSpecialistCabinetAccess(user, profile));
	}

	@Test
	void shouldRejectCabinetWithBlockedActor() {
		User user = actor(1L, UserRole.SPECIALIST, UserStatus.BLOCKED);
		Specialist profile = specialist(1L);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireSpecialistCabinetAccess(user, profile));
	}

	@Test
	void shouldRejectCabinetWithClientRole() {
		User user = actor(1L, UserRole.CLIENT, UserStatus.ACTIVE);
		Specialist profile = specialist(1L);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireSpecialistCabinetAccess(user, profile));
	}

	@Test
	void shouldRejectNullCabinetArguments() {
		User user = actor(1L, UserRole.SPECIALIST, UserStatus.ACTIVE);
		Specialist profile = specialist(1L);

		assertThrows(IllegalArgumentException.class, () -> policy.requireSpecialistCabinetAccess(null, profile));

		assertThrows(IllegalArgumentException.class, () -> policy.requireSpecialistCabinetAccess(user, null));
	}

	// Professional eligibility

	@Test
	void shouldAllowApprovedActiveSpecialist() {
		User user = actor(4L, UserRole.SPECIALIST, UserStatus.ACTIVE);
		Specialist profile = realSpecialist(user);
		profile.approve();

		assertDoesNotThrow(() -> policy.requireProfessionalEligibility(profile));
	}

	@Test
	void shouldRejectPendingSpecialist() {
		User user = actor(4L, UserRole.SPECIALIST, UserStatus.ACTIVE);
		Specialist profile = realSpecialist(user);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireProfessionalEligibility(profile));
	}

	@Test
	void shouldRejectRejectedSpecialist() {
		User user = actor(4L, UserRole.SPECIALIST, UserStatus.ACTIVE);
		Specialist profile = realSpecialist(user);
		profile.reject();

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireProfessionalEligibility(profile));
	}

	@Test
	void shouldRejectSuspendedSpecialist() {
		User user = actor(4L, UserRole.SPECIALIST, UserStatus.ACTIVE);
		Specialist profile = realSpecialist(user);
		profile.approve();
		profile.suspend();

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireProfessionalEligibility(profile));
	}

	@Test
	void shouldRejectApprovedSpecialistWithBlockedUser() {
		User user = actor(4L, UserRole.SPECIALIST, UserStatus.BLOCKED);
		Specialist profile = realSpecialist(user);
		profile.approve();

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireProfessionalEligibility(profile));
	}

	@Test
	void shouldRejectNullProfessionalEligibility() {
		assertThrows(IllegalArgumentException.class, () -> policy.requireProfessionalEligibility(null));
	}

	// Client ownership

	@Test
	void shouldAllowOwnClientResource() {
		User client = actor(1000L, UserRole.CLIENT, UserStatus.ACTIVE);

		assertDoesNotThrow(() -> policy.requireClientOwnership(client, 1000L));
	}

	@Test
	void shouldRejectForeignClientResource() {
		User client = actor(1000L, UserRole.CLIENT, UserStatus.ACTIVE);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireClientOwnership(client, 2000L));
	}

	@Test
	void shouldRejectClientOwnershipWithMissingActorId() {
		User client = actor(null, UserRole.CLIENT, UserStatus.ACTIVE);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireClientOwnership(client, 1000L));
	}

	@Test
	void shouldRejectBlockedClientOwnership() {
		User client = actor(1000L, UserRole.CLIENT, UserStatus.BLOCKED);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireClientOwnership(client, 1000L));
	}

	@Test
	void shouldRejectClientOwnershipForNonClient() {
		User user = actor(1000L, UserRole.SPECIALIST, UserStatus.ACTIVE);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireClientOwnership(user, 1000L));
	}

	@Test
	void shouldRejectNullClientOwnershipArguments() {
		User client = actor(1L, UserRole.CLIENT, UserStatus.ACTIVE);

		assertThrows(IllegalArgumentException.class, () -> policy.requireClientOwnership(null, 1L));

		assertThrows(IllegalArgumentException.class, () -> policy.requireClientOwnership(client, null));
	}

	// Specialist ownership

	@Test
	void shouldAllowOwnSpecialistResource() {
		User user = actor(1000L, UserRole.SPECIALIST, UserStatus.ACTIVE);

		assertDoesNotThrow(() -> policy.requireSpecialistOwnership(user, 1000L));
	}

	@Test
	void shouldRejectForeignSpecialistResource() {
		User user = actor(1000L, UserRole.SPECIALIST, UserStatus.ACTIVE);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireSpecialistOwnership(user, 2000L));
	}

	@Test
	void shouldRejectSpecialistOwnershipWithMissingActorId() {
		User user = actor(null, UserRole.SPECIALIST, UserStatus.ACTIVE);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireSpecialistOwnership(user, 1000L));
	}

	@Test
	void shouldRejectBlockedSpecialistOwnership() {
		User user = actor(1000L, UserRole.SPECIALIST, UserStatus.BLOCKED);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireSpecialistOwnership(user, 1000L));
	}

	@Test
	void shouldRejectSpecialistOwnershipForClient() {
		User user = actor(1000L, UserRole.CLIENT, UserStatus.ACTIVE);

		assertThrows(AuthorizationForbiddenException.class, () -> policy.requireSpecialistOwnership(user, 1000L));
	}

	@Test
	void shouldRejectNullSpecialistOwnershipArguments() {
		User user = actor(1L, UserRole.SPECIALIST, UserStatus.ACTIVE);

		assertThrows(IllegalArgumentException.class, () -> policy.requireSpecialistOwnership(null, 1L));

		assertThrows(IllegalArgumentException.class, () -> policy.requireSpecialistOwnership(user, null));
	}
}