package com.psyweb.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.psyweb.auth.domain.RefreshSession;
import com.psyweb.auth.domain.RefreshToken;
import com.psyweb.auth.repository.RefreshSessionRepository;
import com.psyweb.auth.repository.RefreshTokenRepository;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.service.UserService;

import jakarta.transaction.Transactional;

@Service
public class RefreshSessionService {
	private final Clock clock;
	private final RefreshTokenRepository tokenRepository;
	private final RefreshSessionRepository sessionRepository;
	private final UserService userService;
	
	public RefreshSessionService(Clock clock, RefreshTokenRepository tokenRepository, RefreshSessionRepository sessionRepository, UserService userService) {
		this.clock = clock;
		this.tokenRepository = tokenRepository;
		this.sessionRepository = sessionRepository;
		this.userService = userService;
	}
	
	@Transactional
	public RefreshSession createSession(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("Incorrect user ID");
		}
		User user = userService.getActiveUserForUpdate(userId);
		Instant createdAt = Instant.now(clock);
		RefreshSession session = new RefreshSession(user, createdAt, createdAt.plus(Duration.ofDays(7)));
		return sessionRepository.save(session);
	}
	
	@Transactional
	public RefreshToken createToken(UUID sessionId) {
		
	}
	
	@Transactional
	public RefreshToken rotation(RefreshToken expiredToken) {
		Instant now = Instant.now(clock);
		RefreshSession session = sessionRepository.findForRefreshById(expiredToken.getSessionId()).orElseThrow();
		if (session.getRevokedAt() != null) {
			throw new IllegalArgumentException("Session already revoked");
		}
		if (expiredToken.getConsumedAt() != null) {
			session.addRevokedAt(now);
			throw new IllegalArgumentException("Incorrect token");
		}
		expiredToken.consumeToken(now); 
		session.addLastUsedAt(now);
		RefreshToken newToken = createToken(session.getId());
		return tokenRepository.save(newToken);
	}
	
	public String hashToken() {
		return "";
	}
}
