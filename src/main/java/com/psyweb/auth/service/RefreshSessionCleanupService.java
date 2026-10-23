package com.psyweb.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.psyweb.auth.config.RefreshSessionProperties;
import com.psyweb.auth.domain.RefreshSession;
import com.psyweb.auth.repository.RefreshSessionRepository;

import jakarta.transaction.Transactional;

@Service
public class RefreshSessionCleanupService {
	private final RefreshSessionRepository sessionRepository;
	private final RefreshSessionProperties sessionProperties;
	private final Clock clock;

	public RefreshSessionCleanupService(RefreshSessionRepository sessionRepository,
			RefreshSessionProperties sessionProperties, Clock clock) {
		this.sessionRepository = sessionRepository;
		this.sessionProperties = sessionProperties;
		this.clock = clock;
	}

	@Transactional
	public void cleanup() {
		Instant now = Instant.now(clock);
		List<RefreshSession> sessions = sessionRepository
				.findForCleanup(now.minus(Duration.ofDays(1)), sessionProperties.expirationBatchSize());
		
		for (RefreshSession session : sessions) {
			sessionRepository.deleteById(session.getId());
		}
	}
}
