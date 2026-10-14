package com.psyweb.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.psyweb.auth.validation.PasswordPolicy;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.service.UserService;

@Service
public class RegistrationService {
	private final PasswordEncoder passwordEncoder;
	private final UserService userService;
	
	public RegistrationService(PasswordEncoder passwordEncoder, UserService userService) {
		this.passwordEncoder = passwordEncoder;
		this.userService = userService;
	}
	
	public User register(String email, String rawPassword, UserRole role) {
		String normalizedEmail = User.normalizeEmail(email);
		if (role == null || role != UserRole.CLIENT && role != UserRole.SPECIALIST) {
			throw new InvalidUserDataException("User role must be 'CLIENT' or 'SPECIALIST'");
		}
		PasswordPolicy.validatePassword(rawPassword);
		String encodedPassword = passwordEncoder.encode(rawPassword);
		return userService.createUser(normalizedEmail, encodedPassword, role);
	}
}
