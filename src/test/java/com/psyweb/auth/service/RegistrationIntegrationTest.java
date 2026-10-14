
package com.psyweb.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import com.psyweb.user.exception.UserEmailAlreadyExistsException;
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

	@Test
	void shouldAllowOnlyOneConcurrentRegistrationForSameEmail() throws Exception {

		String email = uniqueEmail();
		String password = "ConcurrentPassword123!";

		CountDownLatch startLatch = new CountDownLatch(1);

		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {

			Future<Object> first = executor.submit(() -> {
				startLatch.await();
				try {
					return registrationService.register(email, password, UserRole.CLIENT);
				} catch (UserEmailAlreadyExistsException e) {
					return e;
				}
			});

			Future<Object> second = executor.submit(() -> {
				startLatch.await();
				try {
					return registrationService.register(email, password, UserRole.CLIENT);
				} catch (UserEmailAlreadyExistsException e) {
					return e;
				}
			});

			startLatch.countDown();

			Object firstResult = first.get(30, TimeUnit.SECONDS);
			Object secondResult = second.get(30, TimeUnit.SECONDS);

			int successfulRegistrations = 0;
			int duplicateEmailErrors = 0;

			for (Object result : new Object[] { firstResult, secondResult }) {
				if (result instanceof User) {
					successfulRegistrations++;
				} else if (result instanceof UserEmailAlreadyExistsException) {
					duplicateEmailErrors++;
				} else {
					fail("Unexpected registration result: " + result);
				}
			}

			assertEquals(1, successfulRegistrations);
			assertEquals(1, duplicateEmailErrors);

			assertTrue(userRepository.findByEmail(email).isPresent());
		}
	}

}
