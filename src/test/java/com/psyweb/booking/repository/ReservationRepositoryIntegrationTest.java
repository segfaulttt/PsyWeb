package com.psyweb.booking.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import org.hibernate.exception.ConstraintViolationException;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

@Transactional
public class ReservationRepositoryIntegrationTest extends PostgreSQLIntegrationTest {

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

	private TestContext createTestContext(String suffix, LocalDateTime now) {
		User specialistUser = userRepository.saveAndFlush(new User("specialist-" + suffix + "@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Ann", "Scheduler", Duration.ZERO, Duration.ZERO));

		User client = userRepository.saveAndFlush(
				new User("client-" + suffix + "@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		return new TestContext(client, specialist, now.plusDays(1));
	}

	private Reservation persistActiveReservation(TestContext context, int slotNumber, LocalDateTime createdAt,
			LocalDateTime expiresAt) {
		LocalDateTime slotStart = context.slotBase().plusHours(slotNumber * 2L);

		AvailabilitySlot slot = new AvailabilitySlot(context.specialist(), slotStart, slotStart.plusHours(1));

		slot.reserve();
		slot = slotRepository.saveAndFlush(slot);

		return reservationRepository.saveAndFlush(new Reservation(context.client(), slot, createdAt, expiresAt));
	}

	private record TestContext(User client, Specialist specialist, LocalDateTime slotBase) {
	}

	@Test
	void shouldRejectSecondActiveReservationForSameSlot() {
		LocalDateTime now = LocalDateTime.now(clock);
		User specialistUser = userRepository.saveAndFlush(
				new User("specialist@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "Smith", Duration.ZERO, Duration.ZERO));

		User client = userRepository
				.saveAndFlush(new User("client@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		LocalDateTime startTime = LocalDateTime.now().plusDays(1);

		AvailabilitySlot slot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, startTime, startTime.plusHours(1)));

		Reservation firstReservation = new Reservation(client, slot, now, now.plusMinutes(5));

		reservationRepository.saveAndFlush(firstReservation);

		Reservation secondReservation = new Reservation(client, slot, now, now.plusMinutes(5));

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
		LocalDateTime now = LocalDateTime.now(clock).withNano(0);

		User specialistUser = userRepository.saveAndFlush(new User("specialist-expiration-query@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "Expiration", Duration.ZERO, Duration.ZERO));

		User client = userRepository.saveAndFlush(
				new User("client-expiration-query@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		LocalDateTime firstSlotStart = now.plusDays(1);

		AvailabilitySlot expiredSlot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, firstSlotStart, firstSlotStart.plusHours(1)));

		AvailabilitySlot boundarySlot = slotRepository.saveAndFlush(
				new AvailabilitySlot(specialist, firstSlotStart.plusHours(2), firstSlotStart.plusHours(3)));

		AvailabilitySlot futureSlot = slotRepository.saveAndFlush(
				new AvailabilitySlot(specialist, firstSlotStart.plusHours(4), firstSlotStart.plusHours(5)));

		AvailabilitySlot cancelledSlot = slotRepository.saveAndFlush(
				new AvailabilitySlot(specialist, firstSlotStart.plusHours(6), firstSlotStart.plusHours(7)));

		Reservation expiredReservation = new Reservation(client, expiredSlot, now.minusMinutes(20),
				now.minusMinutes(10));

		Reservation boundaryReservation = new Reservation(client, boundarySlot, now.minusMinutes(10), now);

		Reservation futureReservation = new Reservation(client, futureSlot, now, now.plusMinutes(10));

		Reservation cancelledReservation = new Reservation(client, cancelledSlot, now.minusMinutes(20),
				now.minusMinutes(10));

		cancelledReservation.cancel();

		reservationRepository.saveAllAndFlush(
				List.of(expiredReservation, boundaryReservation, futureReservation, cancelledReservation));

		List<Reservation> result = reservationRepository.findExpiredBatchForUpdateSkipLocked(now, 100);

		Set<Long> actualReservationIds = result.stream().map(Reservation::getId).collect(Collectors.toSet());

		assertEquals(Set.of(expiredReservation.getId(), boundaryReservation.getId()), actualReservationIds);
	}

	@Test
	public void shouldFindOnlyExpiredActiveReservationsForUpdate() {
		LocalDateTime now = LocalDateTime.now(clock).withNano(0);

		User specialistUser = userRepository.saveAndFlush(new User("specialist-scheduler-query@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "An", "Scheduler", Duration.ZERO, Duration.ZERO));

		User client = userRepository.saveAndFlush(
				new User("client-scheduler-query@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		LocalDateTime firstSlotStart = now.plusDays(1);

		AvailabilitySlot expiredSlot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, firstSlotStart, firstSlotStart.plusHours(1)));

		AvailabilitySlot boundarySlot = slotRepository.saveAndFlush(
				new AvailabilitySlot(specialist, firstSlotStart.plusHours(2), firstSlotStart.plusHours(3)));

		AvailabilitySlot futureSlot = slotRepository.saveAndFlush(
				new AvailabilitySlot(specialist, firstSlotStart.plusHours(4), firstSlotStart.plusHours(5)));

		AvailabilitySlot cancelledSlot = slotRepository.saveAndFlush(
				new AvailabilitySlot(specialist, firstSlotStart.plusHours(6), firstSlotStart.plusHours(7)));

		Reservation expiredActiveReservation = new Reservation(client, expiredSlot, now.minusMinutes(20),
				now.minusMinutes(10));

		Reservation unexpiredActiveReservation = new Reservation(client, futureSlot, now, now.plusMinutes(10));

		Reservation expiredConfirmedReservation = new Reservation(client, boundarySlot, now.minusMinutes(20),
				now.minusMinutes(10));

		Reservation expiredCancelledReservation = new Reservation(client, cancelledSlot, now.minusMinutes(20),
				now.minusMinutes(10));

		expiredCancelledReservation.cancel();

		reservationRepository.saveAllAndFlush(List.of(expiredActiveReservation, unexpiredActiveReservation,
				expiredConfirmedReservation, expiredCancelledReservation));
	}

	@Test
	void shouldIncludeReservationWhenExpiresAtEqualsNow() {
		LocalDateTime now = LocalDateTime.now(clock).withNano(0);
		TestContext context = createTestContext("boundary", now);

		Reservation boundaryReservation = persistActiveReservation(context, 1, now.minusMinutes(10), now);

		List<Reservation> result = reservationRepository.findExpiredBatchForUpdateSkipLocked(now, 100);

		assertEquals(1, result.size());
		assertEquals(boundaryReservation.getId(), result.getFirst().getId());
	}

	@Test
	void shouldReturnExpiredReservationsOrderedByExpiresAtAndId() {
		LocalDateTime now = LocalDateTime.now(clock).withNano(0);
		TestContext context = createTestContext("ordering", now);

		Reservation newest = persistActiveReservation(context, 1, now.minusMinutes(30), now.minusMinutes(5));

		Reservation oldest = persistActiveReservation(context, 2, now.minusMinutes(30), now.minusMinutes(20));

		Reservation sameExpirationFirst = persistActiveReservation(context, 3, now.minusMinutes(30),
				now.minusMinutes(10));

		Reservation sameExpirationSecond = persistActiveReservation(context, 4, now.minusMinutes(30),
				now.minusMinutes(10));

		List<Long> resultIds = reservationRepository.findExpiredBatchForUpdateSkipLocked(now, 100).stream()
				.map(Reservation::getId).toList();

		assertEquals(List.of(oldest.getId(), sameExpirationFirst.getId(), sameExpirationSecond.getId(), newest.getId()),
				resultIds);
	}

	@Test
	void shouldLimitExpiredReservationsToBatchSize() {
		LocalDateTime now = LocalDateTime.now(clock).withNano(0);
		TestContext context = createTestContext("batch-limit", now);

		Reservation first = persistActiveReservation(context, 1, now.minusMinutes(30), now.minusMinutes(20));

		Reservation second = persistActiveReservation(context, 2, now.minusMinutes(30), now.minusMinutes(15));

		persistActiveReservation(context, 3, now.minusMinutes(30), now.minusMinutes(10));

		List<Reservation> result = reservationRepository.findExpiredBatchForUpdateSkipLocked(now, 2);

		assertEquals(2, result.size());
		assertEquals(List.of(first.getId(), second.getId()), result.stream().map(Reservation::getId).toList());
	}
}

