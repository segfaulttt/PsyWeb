package com.psyweb.auth.security;

import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import com.psyweb.user.exception.UserAccessForbiddenException;
import com.psyweb.user.exception.UserNotFoundException;
import com.psyweb.user.service.UserService;

import java.util.function.Supplier;

import org.springframework.security.core.Authentication;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class ActiveUserAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
	private final UserService userService;

	public ActiveUserAuthorizationManager(UserService userService) {
	    this.userService = userService;
	}
	
	@Override
	public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
			RequestAuthorizationContext context) {
		Authentication currentAuthentication = authentication.get();
		if (!(currentAuthentication instanceof JwtAuthenticationToken jwtAuthentication)) {
			return new AuthorizationDecision(false);
		}
		String subject = jwtAuthentication.getToken().getSubject();
		long userId = Long.parseLong(subject);
		
		try {
			userService.getActiveUser(userId);
		} catch (UserAccessForbiddenException | UserNotFoundException e) {
			return new AuthorizationDecision(false);
		}
		
		
		return new AuthorizationDecision(true);
	}
}