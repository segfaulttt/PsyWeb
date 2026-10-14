package com.psyweb.user.service;

import org.springframework.stereotype.Service;

import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.exception.UserAccessForbiddenException;
import com.psyweb.user.exception.UserEmailAlreadyExistsException;
import com.psyweb.user.exception.UserNotFoundException;
import com.psyweb.user.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {
	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
	
	public String validateEmailForCreation(String email) {
		String normalizedEmail = User.normalizeEmail(email);
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new UserEmailAlreadyExistsException("User with this email already exists");
		}
		return normalizedEmail;
	}
	
	@Transactional
	public User createUser(String email, String hashPassword, UserRole role) {
		String normalizedEmail = validateEmailForCreation(email);
		if (role == null || role != UserRole.CLIENT && role != UserRole.SPECIALIST) {
			throw new InvalidUserDataException("User role must be 'CLIENT' or 'SPECIALIST'");
		}
		User user = new User(normalizedEmail, hashPassword, role, UserStatus.ACTIVE);
		return userRepository.save(user);
	}

	public User getUser(Long id) {
		if (id == null) {
			throw new InvalidUserDataException("User id cannot be null");
		}
		User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
		return user;
	}

	@Transactional
	public User changeEmail(Long userId, String newEmail) {
		User user = getActiveUser(userId);

		String normalizedEmail = User.normalizeEmail(newEmail);

		if (userRepository.existsByEmailAndIdNot(normalizedEmail, userId)) {
			throw new UserEmailAlreadyExistsException("Email is already in use");
		}

		user.changeEmail(normalizedEmail);

		return user;
	}

	public User getActiveUser(Long id) {
		User user = getUser(id);
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new UserAccessForbiddenException("User must have status 'ACTIVE'");
		}
		return user;
	}

	public User findUserByEmail(String email) {
		email = User.normalizeEmail(email);
		User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));

		return user;
	}
}
