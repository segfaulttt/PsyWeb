package com.psyweb.user.service;

import org.springframework.stereotype.Service;

import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.exception.UserAccessForbiddenException;
import com.psyweb.user.exception.UserNotFoundException;
import com.psyweb.user.repository.UserRepository;

@Service
public class UserService {
	private final UserRepository userRepository;
	
	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
	
	public User getUser(Long id) {
		if (id == null) {
			throw new InvalidUserDataException("User id cannot be null");
		}
		User user = userRepository.findById(id)
				.orElseThrow(() -> new UserNotFoundException("User not found"));
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
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found"));
		
		return user;
	}
}
