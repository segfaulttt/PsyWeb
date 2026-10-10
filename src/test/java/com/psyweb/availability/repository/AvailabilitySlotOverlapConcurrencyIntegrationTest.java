package com.psyweb.availability.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

import org.postgresql.util.PSQLException;

public class AvailabilitySlotOverlapConcurrencyIntegrationTest extends PostgreSQLIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SpecialistRepository specialistRepository;

	@Autowired
	private AvailabilitySlotRepository slotRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	void shouldAllowOnlyOneConcurrentOverlappingInsert() throws Exception {
		User user = userRepository.saveAndFlush(new User("specialist-concurrent-overlap@example.com", "password-hash",
				UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(user, "Anna", "Concurrent", Duration.ZERO, Duration.ZERO));

		Long specialistId = specialist.getId();

		Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS).plus(Duration.ofDays(30));

		CyclicBarrier barrier = new CyclicBarrier(2);
		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Future<Void> first = executor
					.submit(() -> insertSlotInIndependentTransaction(specialistId, start, start.plus(Duration.ofHours(1)), barrier));

			Future<Void> second = executor.submit(() -> insertSlotInIndependentTransaction(specialistId,
					start.plus(Duration.ofMinutes(30)), start.plus(Duration.ofHours(1)).plus(Duration.ofMinutes(30)), barrier));

			int successfulAttempts = 0;
			int failedAttempts = 0;

			for (Future<Void> future : new Future[] { first, second }) {
				try {
					future.get(10, TimeUnit.SECONDS);
					successfulAttempts++;
				} catch (ExecutionException e) {
					DataIntegrityViolationException exception = findDataIntegrityViolation(e.getCause());

					assertNotNull(exception);
					assertConstraintName(exception, "no_overlapping_active_slots");

					failedAttempts++;
				}
			}

			assertEquals(1, successfulAttempts);
			assertEquals(1, failedAttempts);
		} finally {
			executor.shutdownNow();
		}
	}

	private Void insertSlotInIndependentTransaction(Long specialistId, Instant start, Instant end,
			CyclicBarrier barrier) {

		TransactionTemplate transaction = new TransactionTemplate(transactionManager);

		transaction.executeWithoutResult(status -> {
			Specialist specialist = specialistRepository.findById(specialistId).orElseThrow();

			awaitBarrier(barrier);

			slotRepository.saveAndFlush(new AvailabilitySlot(specialist, start, end));
		});

		return null;
	}

	private void awaitBarrier(CyclicBarrier barrier) {
		try {
			barrier.await(5, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private DataIntegrityViolationException findDataIntegrityViolation(Throwable throwable) {

		Throwable cause = throwable;

		while (cause != null) {
			if (cause instanceof DataIntegrityViolationException exception) {
				return exception;
			}

			cause = cause.getCause();
		}

		return null;
	}

	private void assertConstraintName(DataIntegrityViolationException exception, String expectedConstraintName) {
		Throwable cause = exception;
		while (cause != null) {
			if (cause instanceof ConstraintViolationException constraintException
					&& expectedConstraintName.equals(constraintException.getConstraintName())) {
				return;
			}
			if (cause instanceof PSQLException postgresException && postgresException.getServerErrorMessage() != null
					&& expectedConstraintName.equals(postgresException.getServerErrorMessage().getConstraint())) {
				return;
			}
			cause = cause.getCause();
		}
		throw new AssertionError("Expected constraint violation: " + expectedConstraintName);
	}
}