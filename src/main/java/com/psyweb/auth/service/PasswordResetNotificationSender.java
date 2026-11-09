package com.psyweb.auth.service;

import java.time.Instant;

public interface PasswordResetNotificationSender {

	void sendPasswordReset(String email, String rawToken, Instant expiresAt);
}