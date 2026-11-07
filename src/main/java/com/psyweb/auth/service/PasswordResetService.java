package com.psyweb.auth.service;

public interface PasswordResetService {

	void requestPasswordReset(String email);

	void confirmPasswordReset(String rawToken, String newPassword);
}