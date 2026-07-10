package com.psyweb.user.service;

import org.springframework.stereotype.Service;

import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

@Service
public class UserService {
	private final UserRepository userRepository;
	
	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
	
	public User getActiveUser(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Invalid id");
		}
		User user = userRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new IllegalArgumentException("User must have status 'ACTIVE'");
		}
		return user;
	}
}
