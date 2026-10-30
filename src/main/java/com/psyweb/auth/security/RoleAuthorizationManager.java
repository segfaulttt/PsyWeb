
package com.psyweb.auth.security;

import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import com.psyweb.auth.authorization.AuthorizationPolicy;
import com.psyweb.auth.exception.AuthorizationForbiddenException;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.exception.UserNotFoundException;
import com.psyweb.user.service.UserService;

public class RoleAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

	private final UserService userService;
	private final AuthorizationPolicy authorizationPolicy;
	private final UserRole requiredRole;

	public RoleAuthorizationManager(UserService userService, AuthorizationPolicy authorizationPolicy,
			UserRole requiredRole) {
		this.userService = Objects.requireNonNull(userService, "UserService cannot be null");
		this.authorizationPolicy = Objects.requireNonNull(
		        authorizationPolicy,
		        "AuthorizationPolicy cannot be null"
		);
		this.requiredRole = Objects.requireNonNull(requiredRole, "Required role cannot be null");
	}

	@Override
	public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
			RequestAuthorizationContext context) {
		
		if (authentication == null) {
			return new AuthorizationDecision(false);
		}
		Authentication currentAuthentication = authentication.get();
		
		if (!(currentAuthentication instanceof JwtAuthenticationToken auth)) {
			return new AuthorizationDecision(false);
		}
		String sub = auth.getToken().getSubject();
		if (sub == null || sub.isBlank()) {
			return new AuthorizationDecision(false);
		}
		long userId;
		try {
			userId = Long.parseLong(sub);
		} catch (NumberFormatException e) {
			return new AuthorizationDecision(false);
		}
		if (userId <= 0) {
			return new AuthorizationDecision(false);
		}
		try {
		    User user = userService.getUser(userId);

		    switch (requiredRole) {
		        case CLIENT -> authorizationPolicy.requireActiveClient(user);
		        case SPECIALIST -> authorizationPolicy.requireActiveSpecialist(user);
		        case ADMIN -> authorizationPolicy.requireActiveAdmin(user);
		    }

		    return new AuthorizationDecision(true);
		} catch (UserNotFoundException | AuthorizationForbiddenException e) {
		    return new AuthorizationDecision(false);
		}
	}
}
