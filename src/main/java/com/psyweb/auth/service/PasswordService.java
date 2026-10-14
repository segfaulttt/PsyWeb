package com.psyweb.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.psyweb.auth.exception.InvalidCurrentPasswordException;
import com.psyweb.auth.exception.InvalidPasswordException;
import com.psyweb.auth.validation.PasswordPolicy;
import com.psyweb.user.domain.User;
import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.service.UserService;

@Service
public class PasswordService {
	private final UserService userService;
	private final PasswordEncoder passwordEncoder;
	
	public PasswordService(UserService userService, PasswordEncoder passwordEncoder) {
		this.userService = userService;
		this.passwordEncoder = passwordEncoder;
	}
	
	public void changePassword(Long userId, String currentPassword, String newPassword) {
		if (userId == null) {
			throw new InvalidUserDataException("User ID cannot be null");
		}	
		if (currentPassword == null) {
			throw new InvalidCurrentPasswordException("Current password cannot be null");
		}
		User user = userService.getActiveUser(userId);
		
		String oldHash = user.getPasswordHash();
		
		if (!passwordEncoder.matches(currentPassword, oldHash)) {
			throw new InvalidCurrentPasswordException("Incorrect current password");
		}
		PasswordPolicy.validatePassword(newPassword);
		if (currentPassword.equals(newPassword)) {
			throw new InvalidPasswordException("New password must not be the same as current");
		}
		String newHash = passwordEncoder.encode(newPassword);
		userService.changePassword(userId, oldHash, newHash);
	}
}
