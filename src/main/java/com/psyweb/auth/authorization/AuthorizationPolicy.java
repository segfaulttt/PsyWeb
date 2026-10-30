package com.psyweb.auth.authorization;

import com.psyweb.auth.exception.AuthorizationForbiddenException;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

public class AuthorizationPolicy {

	public void requireActiveClient(User actor) {
		if (actor == null) {
			throw new IllegalArgumentException("Actor cannot be null");
		}
		if (actor.getStatus() != UserStatus.ACTIVE) {
			throw new AuthorizationForbiddenException("Actor must have status 'ACTIVE'");
		}
		if (actor.getRole() != UserRole.CLIENT) {
			throw new AuthorizationForbiddenException("Actor must have role 'CLIENT'");
		}
	}

	public void requireActiveAdmin(User actor) {
		if (actor == null) {
			throw new IllegalArgumentException("Actor cannot be null");
		}
		if (actor.getStatus() != UserStatus.ACTIVE) {
			throw new AuthorizationForbiddenException("Actor must have status 'ACTIVE'");
		}
		if (actor.getRole() != UserRole.ADMIN) {
			throw new AuthorizationForbiddenException("Actor must have role 'ADMIN'");
		}
	}
	
	public void requireActiveSpecialist(User actor) {
		if (actor == null) {
			throw new IllegalArgumentException("Actor cannot be null");
		}
		if (actor.getStatus() != UserStatus.ACTIVE) {
			throw new AuthorizationForbiddenException("Actor must have status 'ACTIVE'");
		}
		if (actor.getRole() != UserRole.SPECIALIST) {
			throw new AuthorizationForbiddenException("Actor must have role 'SPECIALIST'");
		}
	}

	public void requireSpecialistCabinetAccess(User actor, Specialist specialist) {
	    if (specialist == null) {
	        throw new IllegalArgumentException("Specialist cannot be null");
	    }

	    requireSpecialistOwnership(actor, specialist.getId());
	}

	public void requireProfessionalEligibility(Specialist specialist) {
		if (specialist == null) {
			throw new IllegalArgumentException("Specialist cannot be null");
		}
		if (!specialist.isEligible()) {
			throw new AuthorizationForbiddenException("Specialist is not eligible");
		}
	}

	public void requireClientOwnership(User actor, Long resourceClientId) {
		if (resourceClientId == null) {
			throw new IllegalArgumentException("Resource client ID cannot be null");
		}
		requireActiveClient(actor);
		if (actor.getId() == null || !actor.getId().equals(resourceClientId)) {
			throw new AuthorizationForbiddenException("Client resource access denied");
		}
	}

	public void requireSpecialistOwnership(User actor, Long resourceSpecialistId) {
		if (resourceSpecialistId == null) {
			throw new IllegalArgumentException("Resource specialist ID cannot be null");
		}
		requireActiveSpecialist(actor);
		if (actor.getId() == null || !actor.getId().equals(resourceSpecialistId)) {
			throw new AuthorizationForbiddenException("Specialist profile access denied");
		}
	}
}
