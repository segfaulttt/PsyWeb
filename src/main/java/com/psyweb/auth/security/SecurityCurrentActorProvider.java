package com.psyweb.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.psyweb.auth.authorization.CurrentActor;
import com.psyweb.auth.authorization.CurrentActorProvider;
import com.psyweb.auth.exception.UnauthenticatedActorException;
import com.psyweb.user.domain.UserRole;

@Component
public class SecurityCurrentActorProvider implements CurrentActorProvider{
	private final SecurityContextHolderStrategy contextStrategy;
	
	public SecurityCurrentActorProvider(SecurityContextHolderStrategy contextStrategy) {
		this.contextStrategy = contextStrategy;
	}
	
	@Override
	public CurrentActor requireCurrentActor() {
		SecurityContext context = contextStrategy.getContext();
		Authentication auth = context.getAuthentication();
		
		if (!(auth instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) {
			throw new UnauthenticatedActorException("Authentication required");
		}
		Long userId;
		UserRole role;
		try {
			userId = Long.parseLong(token.getToken().getSubject());
			if (userId <= 0) {
				throw new UnauthenticatedActorException("Invalid authentication identity");
			}
		} catch (NumberFormatException e) {
			throw new UnauthenticatedActorException("Invalid authentication identity", e);
		}
		try {
			String userRole = token.getToken().getClaimAsString("role");
			if (userRole == null || userRole.isBlank()) {
				throw new UnauthenticatedActorException("Invalid authentication identity");
			}
			role = UserRole.valueOf(userRole);
		} catch (IllegalArgumentException ie) {
			throw new UnauthenticatedActorException("");
		}
		return new CurrentActor(userId, role);
	}
}
