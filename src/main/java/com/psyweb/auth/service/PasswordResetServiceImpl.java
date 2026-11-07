package com.psyweb.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psyweb.auth.domain.PasswordResetToken;
import com.psyweb.auth.repository.PasswordResetTokenRepository;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.exception.UserNotFoundException;
import com.psyweb.user.service.UserService;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {
	private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);
	private final UserService userService;
	private final PasswordResetTokenRepository tokenRepository;
	private final PasswordResetTokenGenerator tokenGenerator;
	private final PasswordResetNotificationSender notificationSender;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	public PasswordResetServiceImpl(UserService userService, PasswordResetTokenRepository tokenRepository,
			PasswordResetTokenGenerator tokenGenerator, PasswordResetNotificationSender notificationSender,
			PasswordEncoder passwordEncoder, Clock clock) {
		this.userService = userService;
		this.tokenRepository = tokenRepository;
		this.tokenGenerator = tokenGenerator;
		this.notificationSender = notificationSender;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void requestPasswordReset(String email) {
		if (email == null || email.isBlank()) {
			return;
		}
		User user;

		try {
			user = userService.findUserForUpdateByEmail(email);
		} catch (InvalidUserDataException | UserNotFoundException e) {
			return;
		}

		if (user.getStatus() != UserStatus.ACTIVE) {
			return;
		}
		String rawToken = tokenGenerator.generateRawToken();
		String hashToken = tokenGenerator.hashToken(rawToken);
		Instant now = clock.instant();
		Instant expiresAt = now.plus(RESET_TOKEN_TTL);
		PasswordResetToken resetToken = new PasswordResetToken(user, hashToken, now, expiresAt);
		tokenRepository.save(resetToken);
		
	}

	@Override
	public void confirmPasswordReset(String rawToken, String newPassword) {

	}

}