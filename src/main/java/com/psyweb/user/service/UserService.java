package com.psyweb.user.service;

import org.springframework.stereotype.Service;

import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.exception.ConcurrentPasswordChangeException;
import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.exception.InvalidUserStateException;
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

	@Transactional
	public User changePassword(Long userId, String currentPasswordHash, String newPasswordHash) {
		User user = userRepository.findForUpdateById(userId)
				.orElseThrow(() -> new UserNotFoundException("User not found"));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new InvalidUserStateException("User must have status 'ACTIVE'");
		}
		if (!user.getPasswordHash().equals(currentPasswordHash)) {
			throw new ConcurrentPasswordChangeException("Password was modified concurrently");
		}

		user.changePasswordHash(newPasswordHash);

		return user;
	}

	public User getActiveUser(Long id) {
		User user = getUser(id);
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new UserAccessForbiddenException("User must have status 'ACTIVE'");
		}
		return user;
	}

	public User getActiveUserForUpdate(Long userId) {
		if (userId == null) {
			throw new InvalidUserDataException("User ID cannot be null");
		}
		User user = userRepository.findForUpdateById(userId)
				.orElseThrow(() -> new UserNotFoundException("User not found"));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new UserAccessForbiddenException("User must have status 'ACTIVE'");
		}
		return user;
	}

	public User getUserForUpdate(Long userId) {
		if (userId == null) {
			throw new InvalidUserDataException("User ID cannot be null");
		}
		return userRepository.findForUpdateById(userId)
				.orElseThrow(() -> new UserNotFoundException("User not found"));
	}

	public User findUserByEmail(String email) {
		email = User.normalizeEmail(email);
		User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));

		return user;
	}
	
	public User findUserForUpdateByEmail(String email) {
		email = User.normalizeEmail(email);
		User user = userRepository.findForUpdateByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));
		return user;
	}
}
