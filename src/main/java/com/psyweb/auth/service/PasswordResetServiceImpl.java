package com.psyweb.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psyweb.auth.domain.PasswordResetToken;
import com.psyweb.auth.exception.InvalidPasswordResetTokenException;
import com.psyweb.auth.repository.PasswordResetTokenRepository;
import com.psyweb.auth.validation.PasswordPolicy;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.exception.UserNotFoundException;
import com.psyweb.user.service.UserService;

import jakarta.persistence.EntityManager;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {
	private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);
	private final UserService userService;
	private final PasswordResetTokenRepository tokenRepository;
	private final PasswordResetTokenGenerator tokenGenerator;
	private final PasswordResetNotificationSender notificationSender;
	private final PasswordEncoder passwordEncoder;
	private final EntityManager entityManager;
	private final Clock clock;

	public PasswordResetServiceImpl(UserService userService, PasswordResetTokenRepository tokenRepository,
			PasswordResetTokenGenerator tokenGenerator, PasswordResetNotificationSender notificationSender,
			PasswordEncoder passwordEncoder, EntityManager entityManager, Clock clock) {
		this.userService = userService;
		this.tokenRepository = tokenRepository;
		this.tokenGenerator = tokenGenerator;
		this.notificationSender = notificationSender;
		this.passwordEncoder = passwordEncoder;
		this.entityManager = entityManager;
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
		Instant now = clock.instant();
		List<PasswordResetToken> resetTokens = tokenRepository.findUnresolvedByUserId(user.getId());
		for (PasswordResetToken token : resetTokens) {
			token.revoke(now);
		}
		String rawToken = tokenGenerator.generateRawToken();
		String hashToken = tokenGenerator.hashToken(rawToken);
		Instant expiresAt = now.plus(RESET_TOKEN_TTL);
		PasswordResetToken resetToken = new PasswordResetToken(user, hashToken, now, expiresAt);
		tokenRepository.save(resetToken);

	}

	@Override
	@Transactional
	public void confirmPasswordReset(String rawToken, String newPassword) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new InvalidPasswordResetTokenException();
		}
		PasswordPolicy.validatePassword(newPassword);
		String hashToken = tokenGenerator.hashToken(rawToken);
		PasswordResetToken resetToken = tokenRepository.findByTokenHash(hashToken)
				.orElseThrow(() -> new InvalidPasswordResetTokenException());
		User user = userService.getUserForUpdate(resetToken.getUserId());
		PasswordResetToken token = tokenRepository.findForUpdateById(resetToken.getId())
				.orElseThrow(() -> new InvalidPasswordResetTokenException());
		entityManager.refresh(token);
		Instant now = clock.instant();
		if (!token.isUsableAt(now)) {
			throw new InvalidPasswordResetTokenException();
		}
		String newPasswordHash = passwordEncoder.encode(newPassword);
		userService.resetPasswordHash(user.getId(), newPasswordHash);
		token.consume(now);
		List<PasswordResetToken> resetTokens = tokenRepository.findUnresolvedByUserId(user.getId());
		for (PasswordResetToken t : resetTokens) {
			if (!t.getId().equals(token.getId())) {
				t.revoke(now);
			}
		}
	}

}