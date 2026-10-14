
package com.psyweb.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.exception.ConcurrentPasswordChangeException;
import com.psyweb.user.repository.UserRepository;
import com.psyweb.user.service.UserService;

class PasswordIntegrationTest extends PostgreSQLIntegrationTest {

	@Autowired
	private PasswordService passwordService;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private static final String CURRENT_PASSWORD = "CurrentPassword123!";

	private static final String NEW_PASSWORD = "NewStrongPassword123!";

	private String uniqueEmail() {
		return "password-" + UUID.randomUUID() + "@example.com";
	}

	@Test
	void shouldPersistNewPasswordHash() {
		User registered = registrationService.register(uniqueEmail(), CURRENT_PASSWORD, UserRole.CLIENT);

		Long userId = registered.getId();
		String previousHash = registered.getPasswordHash();

		passwordService.changePassword(userId, CURRENT_PASSWORD, NEW_PASSWORD);

		User persisted = userRepository.findById(userId).orElseThrow();

		assertNotEquals(previousHash, persisted.getPasswordHash());

		assertTrue(passwordEncoder.matches(NEW_PASSWORD, persisted.getPasswordHash()));

		assertFalse(passwordEncoder.matches(CURRENT_PASSWORD, persisted.getPasswordHash()));

		assertTrue(persisted.getPasswordHash().startsWith("$argon2id$"));
	}

	@Test
	void shouldKeepPasswordUnchangedWhenCurrentPasswordIsWrong() {
		User registered = registrationService.register(uniqueEmail(), CURRENT_PASSWORD, UserRole.CLIENT);

		String previousHash = registered.getPasswordHash();

		assertThrows(com.psyweb.auth.exception.InvalidCurrentPasswordException.class,
				() -> passwordService.changePassword(registered.getId(), "IncorrectPassword123!", NEW_PASSWORD));

		User persisted = userRepository.findById(registered.getId()).orElseThrow();

		assertEquals(previousHash, persisted.getPasswordHash());
	}

	@Test
	void shouldRejectStalePasswordHashWithoutOverwriting() {
		User registered = registrationService.register(uniqueEmail(), CURRENT_PASSWORD, UserRole.CLIENT);

		Long userId = registered.getId();
		String oldHash = registered.getPasswordHash();

		passwordService.changePassword(userId, CURRENT_PASSWORD, NEW_PASSWORD);

		User updated = userRepository.findById(userId).orElseThrow();

		String updatedHash = updated.getPasswordHash();

		assertThrows(ConcurrentPasswordChangeException.class,
				() -> userService.changePassword(userId, oldHash, "another-encoded-hash"));

		User persisted = userRepository.findById(userId).orElseThrow();

		assertEquals(updatedHash, persisted.getPasswordHash());

		assertTrue(passwordEncoder.matches(NEW_PASSWORD, persisted.getPasswordHash()));
	}
}
