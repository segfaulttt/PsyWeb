package com.psyweb.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.psyweb.auth.service.dto.AuthenticationResult;
import com.psyweb.auth.service.dto.IssuedAccessToken;
import com.psyweb.auth.service.dto.IssuedRefreshToken;
import com.psyweb.auth.service.dto.RefreshRotationResult;
import com.psyweb.auth.service.dto.Success;
import com.psyweb.user.domain.User;
import com.psyweb.user.service.UserService;

import jakarta.transaction.Transactional;

@Service
public class AuthService {
	private final UserService userService;
	private final AccessTokenService accessTokenService;
	private final RefreshSessionService refreshSessionService;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserService userService, AccessTokenService accessTokenService,
			RefreshSessionService refreshSessionService, PasswordEncoder passwordEncoder) {
		this.accessTokenService = accessTokenService;
		this.userService = userService;
		this.refreshSessionService = refreshSessionService;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public AuthenticationResult login(String email, String rawPassword) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Invalid email");
		}
		if (rawPassword == null || rawPassword.isBlank()) {
			throw new IllegalArgumentException("Invalid password");
		}

		User user = userService.findUserForUpdateByEmail(email);

		if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
			throw new IllegalArgumentException("Incorrect password");
		}

		IssuedRefreshToken refreshToken = refreshSessionService.createSession(user.getId());
		IssuedAccessToken accessToken = accessTokenService.generateAccessToken(user.getId(), user.getRole());

		return new AuthenticationResult(accessToken.accessToken(), refreshToken.rawRefreshToken(),
				accessToken.expiresAt());
	}

	public AuthenticationResult refresh(String rawRefreshToken) {
		RefreshRotationResult result = refreshSessionService.rotation(rawRefreshToken);
		if (result instanceof Success res) {
			IssuedAccessToken accessToken = accessTokenService.generateAccessToken(res.authenticatedUser().userId(),
					res.authenticatedUser().role());
			return new AuthenticationResult(accessToken.accessToken(), res.issuedRefreshToken().rawRefreshToken(),
					accessToken.expiresAt());
		}
		throw new IllegalArgumentException();
	}

	public void logout(String rawRefreshToken) {
		refreshSessionService.logout(rawRefreshToken);
	}

	public void logoutAll(Long userId) {
		refreshSessionService.logoutAll(userId);
	}
}
