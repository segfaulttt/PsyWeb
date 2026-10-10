package com.psyweb.booking.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import org.hibernate.exception.ConstraintViolationException;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.domain.ReservationStatus;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Transactional
public class ReservationRepositoryIntegrationTest extends PostgreSQLIntegrationTest {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Clock clock;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SpecialistRepository specialistRepository;

	@Autowired
	private AvailabilitySlotRepository slotRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	private TestContext createTestContext(String suffix, Instant now) {
		User specialistUser = userRepository.saveAndFlush(new User("specialist-" + suffix + "@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Ann", "Scheduler", Duration.ZERO, Duration.ZERO));

		User client = userRepository.saveAndFlush(
				new User("client-" + suffix + "@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		return new TestContext(client, specialist, now.plus(Duration.ofDays(1)));
	}

	private Reservation persistActiveReservation(TestContext context, int slotNumber, Instant createdAt,
			Instant expiresAt) {
		Instant slotStart = context.slotBase().plus(Duration.ofHours(slotNumber * 2L));

		AvailabilitySlot slot = new AvailabilitySlot(context.specialist(), slotStart, slotStart.plus(Duration.ofHours(1)));

		slot.reserve();
		slot = slotRepository.saveAndFlush(slot);

		return reservationRepository.saveAndFlush(new Reservation(context.client(), slot, createdAt, expiresAt));
	}

	private record TestContext(User client, Specialist specialist, Instant slotBase) {
	}

	@Test
	void shouldRejectSecondActiveReservationForSameSlot() {
		Instant now = clock.instant();
		User specialistUser = userRepository.saveAndFlush(
				new User("specialist@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "Smith", Duration.ZERO, Duration.ZERO));

		User client = userRepository
				.saveAndFlush(new User("client@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		Instant startTime = clock.instant().plus(Duration.ofDays(1));

		AvailabilitySlot slot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, startTime, startTime.plus(Duration.ofHours(1))));

		Reservation firstReservation = new Reservation(client, slot, now, now.plus(Duration.ofMinutes(5)));

		reservationRepository.saveAndFlush(firstReservation);

		Reservation secondReservation = new Reservation(client, slot, now, now.plus(Duration.ofMinutes(5)));

		DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
				() -> reservationRepository.saveAndFlush(secondReservation));

		assertTrue(containsConstraintViolation(exception, "unique_active_reservation_slot"));
	}

	private boolean containsConstraintViolation(Throwable exception, String expectedConstraintName) {
		Throwable cause = exception;

		while (cause != null) {
			if (cause instanceof ConstraintViolationException constraintException
					&& expectedConstraintName.equals(constraintException.getConstraintName())) {
				return true;
			}
			cause = cause.getCause();
		}
		return false;
	}

	@Test
	void shouldFindOnlyExpiredActiveReservations() {
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);

		User specialistUser = userRepository.saveAndFlush(new User("specialist-expiration-query@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "Expiration", Duration.ZERO, Duration.ZERO));

		User client = userRepository.saveAndFlush(
				new User("client-expiration-query@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		Instant firstSlotStart = now.plus(Duration.ofDays(1));

		AvailabilitySlot expiredSlot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, firstSlotStart, firstSlotStart.plus(Duration.ofHours(1))));

		AvailabilitySlot boundarySlot = slotRepository.saveAndFlush(
				new AvailabilitySlot(specialist, firstSlotStart.plus(Duration.ofHours(2)), firstSlotStart.plus(Duration.ofHours(3))));

		AvailabilitySlot futureSlot = slotRepository.saveAndFlush(
				new AvailabilitySlot(specialist, firstSlotStart.plus(Duration.ofHours(4)), firstSlotStart.plus(Duration.ofHours(5))));

		AvailabilitySlot cancelledSlot = slotRepository.saveAndFlush(
				new AvailabilitySlot(specialist, firstSlotStart.plus(Duration.ofHours(6)), firstSlotStart.plus(Duration.ofHours(7))));

		Reservation expiredReservation = new Reservation(client, expiredSlot, now.minus(Duration.ofMinutes(20)),
				now.minus(Duration.ofMinutes(10)));

		Reservation boundaryReservation = new Reservation(client, boundarySlot, now.minus(Duration.ofMinutes(10)), now);

		Reservation futureReservation = new Reservation(client, futureSlot, now, now.plus(Duration.ofMinutes(10)));

		Reservation cancelledReservation = new Reservation(client, cancelledSlot, now.minus(Duration.ofMinutes(20)),
				now.minus(Duration.ofMinutes(10)));

		cancelledReservation.cancel(now.minus(Duration.ofMinutes(15)), CancellationInitiator.CLIENT,
				CancellationReason.CLIENT_REQUEST);

		reservationRepository.saveAllAndFlush(
				List.of(expiredReservation, boundaryReservation, futureReservation, cancelledReservation));

		List<Reservation> result = reservationRepository.findExpiredBatchForUpdateSkipLocked(now, 100);

		Set<Long> actualReservationIds = result.stream().map(Reservation::getId).collect(Collectors.toSet());

		assertEquals(Set.of(expiredReservation.getId(), boundaryReservation.getId()), actualReservationIds);
	}

	@Test
	void shouldIncludeReservationWhenExpiresAtEqualsNow() {
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		TestContext context = createTestContext("boundary", now);

		Reservation boundaryReservation = persistActiveReservation(context, 1, now.minus(Duration.ofMinutes(10)), now);

		List<Reservation> result = reservationRepository.findExpiredBatchForUpdateSkipLocked(now, 100);

		assertEquals(1, result.size());
		assertEquals(boundaryReservation.getId(), result.getFirst().getId());
	}

	@Test
	void shouldReturnExpiredReservationsOrderedByExpiresAtAndId() {
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		TestContext context = createTestContext("ordering", now);

		Reservation newest = persistActiveReservation(context, 1, now.minus(Duration.ofMinutes(30)), now.minus(Duration.ofMinutes(5)));

		Reservation oldest = persistActiveReservation(context, 2, now.minus(Duration.ofMinutes(30)), now.minus(Duration.ofMinutes(20)));

		Reservation sameExpirationFirst = persistActiveReservation(context, 3, now.minus(Duration.ofMinutes(30)),
				now.minus(Duration.ofMinutes(10)));

		Reservation sameExpirationSecond = persistActiveReservation(context, 4, now.minus(Duration.ofMinutes(30)),
				now.minus(Duration.ofMinutes(10)));

		List<Long> resultIds = reservationRepository.findExpiredBatchForUpdateSkipLocked(now, 100).stream()
				.map(Reservation::getId).toList();

		assertEquals(List.of(oldest.getId(), sameExpirationFirst.getId(), sameExpirationSecond.getId(), newest.getId()),
				resultIds);
	}

	@Test
	void shouldLimitExpiredReservationsToBatchSize() {
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		TestContext context = createTestContext("batch-limit", now);

		Reservation first = persistActiveReservation(context, 1, now.minus(Duration.ofMinutes(30)), now.minus(Duration.ofMinutes(20)));

		Reservation second = persistActiveReservation(context, 2, now.minus(Duration.ofMinutes(30)), now.minus(Duration.ofMinutes(15)));

		persistActiveReservation(context, 3, now.minus(Duration.ofMinutes(30)), now.minus(Duration.ofMinutes(10)));

		List<Reservation> result = reservationRepository.findExpiredBatchForUpdateSkipLocked(now, 2);

		assertEquals(2, result.size());
		assertEquals(List.of(first.getId(), second.getId()), result.stream().map(Reservation::getId).toList());
	}

	@Test
	public void shouldPersistReservationCancellationMetadata() {
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		Instant startTime = now.plus(Duration.ofDays(1));
		Instant createdAt = now.minus(Duration.ofMinutes(5));
		Instant expiresAt = now.plus(Duration.ofMinutes(10));
		Instant cancelledAt = now;

		User specialistUser = userRepository.saveAndFlush(new User("specialist-reservation-metadata@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository.saveAndFlush(
				new Specialist(specialistUser, "Anna", "ReservationMetadata", Duration.ZERO, Duration.ZERO));

		User client = userRepository.saveAndFlush(new User("client-reservation-metadata@example.com", "password-hash",
				UserRole.CLIENT, UserStatus.ACTIVE));

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, startTime.plus(Duration.ofHours(1)));

		slot.reserve();
		slot = slotRepository.saveAndFlush(slot);

		Reservation reservation = new Reservation(client, slot, createdAt, expiresAt);

		reservation.cancel(cancelledAt, CancellationInitiator.CLIENT, CancellationReason.CLIENT_REQUEST);

		reservation = reservationRepository.saveAndFlush(reservation);

		Long reservationId = reservation.getId();

		entityManager.clear();

		Reservation result = reservationRepository.findById(reservationId).orElseThrow();

		assertEquals(reservationId, result.getId());
		assertEquals(ReservationStatus.CANCELLED, result.getStatus());
		assertEquals(cancelledAt, result.getCancelledAt());
		assertEquals(CancellationInitiator.CLIENT, result.getCancellationInitiator());
		assertEquals(CancellationReason.CLIENT_REQUEST, result.getCancellationReason());
	}

	@Test
	public void shouldRejectPartialReservationCancellationMetadata() {
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		Instant startTime = now.plus(Duration.ofDays(1));
		Instant createdAt = now.minus(Duration.ofMinutes(5));
		Instant expiresAt = now.plus(Duration.ofMinutes(10));
		Instant cancelledAt = now;

		User specialistUser = userRepository.saveAndFlush(new User("specialist-reservation-partial@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository.saveAndFlush(
				new Specialist(specialistUser, "Anna", "ReservationPartial", Duration.ZERO, Duration.ZERO));

		User client = userRepository.saveAndFlush(new User("client-reservation-partial@example.com", "password-hash",
				UserRole.CLIENT, UserStatus.ACTIVE));

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, startTime.plus(Duration.ofHours(1)));

		slot.reserve();
		slot = slotRepository.saveAndFlush(slot);

		Reservation reservation = reservationRepository
				.saveAndFlush(new Reservation(client, slot, createdAt, expiresAt));

		Long reservationId = reservation.getId();

		assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
				UPDATE reservations
				SET cancelled_at = ?
				WHERE id = ?
				""", cancelledAt.atOffset(ZoneOffset.UTC), reservationId));
	}
}
