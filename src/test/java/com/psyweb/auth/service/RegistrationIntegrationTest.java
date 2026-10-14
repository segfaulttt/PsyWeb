
package com.psyweb.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

class RegistrationIntegrationTest extends PostgreSQLIntegrationTest {

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private String uniqueEmail() {
		return "registration-" + UUID.randomUUID() + "@example.com";
	}

	@Test
	void shouldPersistUserWithArgon2PasswordHash() {
		String email = uniqueEmail();
		String rawPassword = "VeryStrongPassword123!";

		User registered = registrationService.register(email, rawPassword, UserRole.CLIENT);

		assertNotNull(registered.getId());

		User persisted = new TransactionTemplate(transactionManager).execute(status -> {
			entityManager.clear();
			return userRepository.findById(registered.getId()).orElseThrow();
		});

		assertNotNull(persisted);
		assertEquals(email, persisted.getEmail());
		assertEquals(UserRole.CLIENT, persisted.getRole());

		assertNotEquals(rawPassword, persisted.getPasswordHash());
		assertTrue(persisted.getPasswordHash().startsWith("$argon2id$"));
	}

	@Test
	void shouldVerifyStoredPasswordUsingPasswordEncoder() {
		String email = uniqueEmail();
		String rawPassword = "AnotherStrongPassword123!";

		User registered = registrationService.register(email, rawPassword, UserRole.CLIENT);

		User persisted = userRepository.findById(registered.getId()).orElseThrow();

		assertTrue(passwordEncoder.matches(rawPassword, persisted.getPasswordHash()));

		assertFalse(passwordEncoder.matches("IncorrectPassword123!", persisted.getPasswordHash()));
	}

	@Test
	void shouldRejectDuplicateEmailInDatabase() {
		String email = uniqueEmail();

		User first = new User(email, "first-test-hash", UserRole.CLIENT, UserStatus.ACTIVE);

		userRepository.saveAndFlush(first);

		User duplicate = new User(email, "second-test-hash", UserRole.CLIENT, UserStatus.ACTIVE);

		assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(duplicate));
	}

	@Test
	void shouldCreateUnverifiedUserWithActiveStatus() {
		String email = uniqueEmail();

		User registered = registrationService.register(email, "UnverifiedPassword123!", UserRole.SPECIALIST);

		User persisted = userRepository.findById(registered.getId()).orElseThrow();

		assertEquals(UserStatus.ACTIVE, persisted.getStatus());
		assertNull(persisted.getEmailVerifiedAt());
		assertEquals(UserRole.SPECIALIST, persisted.getRole());
	}
}
