package com.psyweb.auth.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefreshSessionCleanupScheduler {
	private final RefreshSessionCleanupService refreshSessionCleanupService;
	
	public RefreshSessionCleanupScheduler(RefreshSessionCleanupService refreshSessionCleanupService) {
		this.refreshSessionCleanupService = refreshSessionCleanupService;
	}
	
	@Scheduled(fixedDelayString = "${psyweb.auth.expiration-scan-interval}")
	public void expireSessions() {
		refreshSessionCleanupService.cleanup();
	}
}
