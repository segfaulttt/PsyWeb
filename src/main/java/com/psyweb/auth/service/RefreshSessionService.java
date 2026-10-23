package com.psyweb.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.psyweb.auth.domain.RefreshSession;
import com.psyweb.auth.domain.RefreshToken;
import com.psyweb.auth.repository.RefreshSessionRepository;
import com.psyweb.auth.repository.RefreshTokenRepository;
import com.psyweb.auth.service.dto.IssuedRefreshToken;
import com.psyweb.auth.service.dto.RefreshRotationResult;
import com.psyweb.auth.service.dto.ReplayDetected;
import com.psyweb.auth.service.dto.Success;
import com.psyweb.user.domain.User;
import com.psyweb.user.service.UserService;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Service
public class RefreshSessionService {
	private final Clock clock;
	private final RefreshTokenRepository tokenRepository;
	private final RefreshSessionRepository sessionRepository;
	private final UserService userService;
	private final RefreshTokenGenerator tokenGenerator;
	private final EntityManager entityManager;

	public RefreshSessionService(Clock clock, RefreshTokenRepository tokenRepository,
			RefreshSessionRepository sessionRepository, UserService userService, RefreshTokenGenerator tokenGenerator,
			EntityManager entityManager) {
		this.clock = clock;
		this.tokenRepository = tokenRepository;
		this.sessionRepository = sessionRepository;
		this.userService = userService;
		this.tokenGenerator = tokenGenerator;
		this.entityManager = entityManager;
	}

	@Transactional
	public IssuedRefreshToken createSession(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("Incorrect user ID");
		}
		User user = userService.getActiveUserForUpdate(userId);
		Instant createdAt = Instant.now(clock);
		RefreshSession session = new RefreshSession(user, createdAt, createdAt.plus(Duration.ofDays(7)));
		sessionRepository.save(session);
		String rawToken = createRefreshToken(session);
		return new IssuedRefreshToken(rawToken, session.getExpiresAt());
	}

	private String createRefreshToken(RefreshSession session) {
		if (session == null) {
			throw new IllegalArgumentException("Incorrect session");
		}
		Instant now = Instant.now(clock);
		String rawToken = tokenGenerator.generateRawRefreshToken();
		RefreshToken token = new RefreshToken(session, tokenGenerator.hashToken(rawToken), now);
		tokenRepository.save(token);
		return rawToken;
	}

	@Transactional
	public RefreshRotationResult rotation(String rawRefreshToken) {
		if (rawRefreshToken == null) {
			throw new IllegalArgumentException("Incorrect refresh token");
		}
		String hashedToken = tokenGenerator.hashToken(rawRefreshToken);
		RefreshToken oldToken = tokenRepository.findByTokenHash(hashedToken)
				.orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));
		userService.getActiveUserForUpdate(sessionRepository.findById(oldToken.getSessionId()).orElseThrow().getUserId());
		RefreshSession session = sessionRepository.findForRefreshById(oldToken.getSessionId())
				.orElseThrow(() -> new IllegalArgumentException("Session not found"));
		oldToken = tokenRepository.findForRotationById(oldToken.getId())
				.orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));
		entityManager.refresh(session);
		entityManager.refresh(oldToken);
		Instant now = Instant.now(clock);
		if (!oldToken.getSessionId().equals(session.getId())) {
			throw new IllegalArgumentException("Refresh token must belong to session");
		}

		if (session.getRevokedAt() != null) {
			throw new IllegalArgumentException("Session already revoked");
		}

		if (!session.getExpiresAt().isAfter(now)) {
			throw new IllegalArgumentException("Session already expired");
		}

		if (oldToken.getConsumedAt() != null) {
			session.addRevokedAt(now);
			return new ReplayDetected();
		}
		oldToken.consumeToken(now);
		session.addLastUsedAt(now);
		entityManager.flush();
		String newRawToken = createRefreshToken(session);
		return new Success(new IssuedRefreshToken(newRawToken, session.getExpiresAt()));
	}
	
	@Transactional
	public void logout(String rawRefreshToken) {
		if (rawRefreshToken == null) {
			throw new IllegalArgumentException("Incorrect refresh token");
		}
		String hashedToken = tokenGenerator.hashToken(rawRefreshToken);
		Optional<RefreshToken> tokenOpt = tokenRepository.findByTokenHash(hashedToken);
		if (tokenOpt.isEmpty()) {
			return;
		}
		RefreshToken oldToken = tokenOpt.get();
		userService.getUserForUpdate(sessionRepository.findById(oldToken.getSessionId()).orElseThrow().getUserId());
		RefreshSession session = sessionRepository.findForRefreshById(oldToken.getSessionId())
				.orElseThrow(() -> new IllegalArgumentException("Session not found"));
		oldToken = tokenRepository.findForRotationById(oldToken.getId())
				.orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));
		entityManager.refresh(session);
		entityManager.refresh(oldToken);
		Instant now = Instant.now(clock);
	
		if (session.getRevokedAt() != null) {
			return;
		}
		
		session.addRevokedAt(now);
	}
	
	@Transactional
	public void logoutAll(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("User ID cannot be null");
		}
		User user = userService.getUserForUpdate(userId);
		List<RefreshSession> sessions = sessionRepository.findForLogoutByUserId(user.getId());
		Instant now = Instant.now(clock);
		for (RefreshSession s : sessions) {
			if (s.getRevokedAt() == null) {
				s.addRevokedAt(now);
			}
		}
	}
}
