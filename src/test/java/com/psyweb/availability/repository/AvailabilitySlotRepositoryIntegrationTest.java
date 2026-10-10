package com.psyweb.availability.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.postgresql.util.PSQLException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.service.AvailabilitySlotService;
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
import jakarta.transaction.Transactional;

@Transactional
public class AvailabilitySlotRepositoryIntegrationTest extends PostgreSQLIntegrationTest {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Clock clock;

	@Autowired
	UserRepository userRepository;

	@Autowired
	SpecialistRepository specialistRepository;

	@Autowired
	AvailabilitySlotRepository slotRepository;

	@Autowired
	private AvailabilitySlotService slotService;

	@Test
	public void shouldPersistAvailabilitySlotCancellationMetadata() {
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		Instant startTime = now.plus(Duration.ofDays(1));
		Instant cancelledAt = now.plus(Duration.ofMinutes(5));

		User specialistUser = userRepository.saveAndFlush(new User("specialist-slot-metadata@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "SlotMetadata", Duration.ZERO, Duration.ZERO));

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, startTime.plus(Duration.ofHours(1)));

		slot.cancel(cancelledAt, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		slot = slotRepository.saveAndFlush(slot);

		Long slotId = slot.getId();

		entityManager.clear();

		AvailabilitySlot result = slotRepository.findById(slotId).orElseThrow();

		assertEquals(slotId, result.getId());
		assertEquals(cancelledAt, result.getCancelledAt());
		assertEquals(AvailabilityStatus.CANCELLED, result.getAvailabilityStatus());
		assertEquals(CancellationInitiator.SPECIALIST, result.getCancellationInitiator());
		assertEquals(CancellationReason.SPECIALIST_REMOVED_AVAILABILITY, result.getCancellationReason());
	}

	@Test
	public void shouldRejectPartialAvailabilitySlotCancellationMetadata() {
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		Instant startTime = now.plus(Duration.ofDays(1));
		Instant cancelledAt = now.plus(Duration.ofMinutes(5));

		User specialistUser = userRepository.saveAndFlush(new User("specialist-slot-partial@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "SlotPartial", Duration.ZERO, Duration.ZERO));

		AvailabilitySlot slot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, startTime, startTime.plus(Duration.ofHours(1))));

		Long slotId = slot.getId();

		assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
				UPDATE slots
				SET cancelled_at = ?
				WHERE id = ?
				""", cancelledAt.atOffset(ZoneOffset.UTC), slotId));
	}

	@Test
	void shouldAllowCreatingNewSlotForCancelledTime() {
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		Instant startTime = now.plus(Duration.ofDays(1));
		Instant endTime = startTime.plus(Duration.ofHours(1));

		User specialistUser = userRepository.saveAndFlush(new User("specialist-reopen-slot@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = new Specialist(specialistUser, "Anna", "Reopen", Duration.ZERO, Duration.ZERO);

		specialist.approve();
		specialist = specialistRepository.saveAndFlush(specialist);

		AvailabilitySlot firstSlot = slotService.createSlot(specialist.getId(), startTime, endTime);

		slotService.cancelSlot(firstSlot.getId(), now, CancellationInitiator.SPECIALIST,
				CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		AvailabilitySlot reopenedSlot = slotService.createSlot(specialist.getId(), startTime, endTime);

		AvailabilitySlot cancelledSlot = slotRepository.findById(firstSlot.getId()).orElseThrow();

		assertEquals(AvailabilityStatus.CANCELLED, cancelledSlot.getAvailabilityStatus());

		assertEquals(AvailabilityStatus.FREE, reopenedSlot.getAvailabilityStatus());

		assertNotEquals(firstSlot.getId(), reopenedSlot.getId());
	}

	@Test
	void shouldFindOnlySlotsStartingAfterSuspensionTime() {
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		Instant suspendedAt = now.plus(Duration.ofDays(2));

		User specialistUser = userRepository.saveAndFlush(new User("specialist-suspension-boundary@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = new Specialist(specialistUser, "Anna", "Boundary", Duration.ZERO, Duration.ZERO);

		specialist.approve();
		specialist = specialistRepository.saveAndFlush(specialist);

		AvailabilitySlot beforeBoundary = new AvailabilitySlot(specialist, suspendedAt.minus(Duration.ofHours(1)), suspendedAt);

		AvailabilitySlot atBoundary = new AvailabilitySlot(specialist, suspendedAt, suspendedAt.plus(Duration.ofHours(1)));

		AvailabilitySlot afterBoundary = new AvailabilitySlot(specialist, suspendedAt.plus(Duration.ofHours(1)),
				suspendedAt.plus(Duration.ofHours(2)));

		slotRepository.saveAllAndFlush(List.of(beforeBoundary, atBoundary, afterBoundary));

		entityManager.clear();

		List<AvailabilitySlot> result = slotRepository.findFutureForUpdateBySpecialistId(specialist.getId(),
				suspendedAt);

		assertEquals(1, result.size());
		assertEquals(afterBoundary.getId(), result.get(0).getId());
	}

	@ParameterizedTest
	@EnumSource(value = AvailabilityStatus.class, names = { "FREE", "RESERVED", "BOOKED" })
	void shouldRejectOverlappingActiveSlotsForSameSpecialist(AvailabilityStatus existingStatus) {

		Specialist specialist = persistSpecialist("active-" + existingStatus.name().toLowerCase());

		Instant start = clock.instant().truncatedTo(ChronoUnit.SECONDS).plus(Duration.ofDays(10));

		AvailabilitySlot existingSlot = createSlotWithStatus(specialist, start, start.plus(Duration.ofHours(1)), existingStatus);

		slotRepository.saveAndFlush(existingSlot);

		AvailabilitySlot overlappingSlot = new AvailabilitySlot(specialist, start.plus(Duration.ofMinutes(30)),
				start.plus(Duration.ofHours(1)).plus(Duration.ofMinutes(30)));

		DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
				() -> slotRepository.saveAndFlush(overlappingSlot));

		assertConstraintName(exception, "no_overlapping_active_slots");
	}

	@Test
	void shouldAllowAdjacentActiveSlotsForSameSpecialist() {
		Specialist specialist = persistSpecialist("adjacent"); 

		Instant start = clock.instant().truncatedTo(ChronoUnit.SECONDS).plus(Duration.ofDays(11));

		AvailabilitySlot firstSlot = new AvailabilitySlot(specialist, start, start.plus(Duration.ofHours(1)));

		AvailabilitySlot secondSlot = new AvailabilitySlot(specialist, start.plus(Duration.ofHours(1)), start.plus(Duration.ofHours(2)));

		firstSlot = slotRepository.saveAndFlush(firstSlot);
		secondSlot = slotRepository.saveAndFlush(secondSlot);

		assertNotNull(firstSlot.getId());
		assertNotNull(secondSlot.getId());
	}

	@Test
	void shouldAllowOverlappingSlotsForDifferentSpecialists() {
		Specialist firstSpecialist = persistSpecialist("different-first");
		Specialist secondSpecialist = persistSpecialist("different-second");

		Instant start = clock.instant().truncatedTo(ChronoUnit.SECONDS).plus(Duration.ofDays(12));

		AvailabilitySlot firstSlot = new AvailabilitySlot(firstSpecialist, start, start.plus(Duration.ofHours(1)));

		AvailabilitySlot secondSlot = new AvailabilitySlot(secondSpecialist, start, start.plus(Duration.ofHours(1)));

		firstSlot = slotRepository.saveAndFlush(firstSlot);
		secondSlot = slotRepository.saveAndFlush(secondSlot);

		assertNotNull(firstSlot.getId());
		assertNotNull(secondSlot.getId());
	}

	private Specialist persistSpecialist(String suffix) {
		User specialistUser = userRepository.saveAndFlush(new User("specialist-overlap-" + suffix + "@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		return specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "Overlap", Duration.ZERO, Duration.ZERO));
	}

	private AvailabilitySlot createSlotWithStatus(Specialist specialist, Instant start, Instant end,
			AvailabilityStatus status) {

		AvailabilitySlot slot = new AvailabilitySlot(specialist, start, end);

		if (status == AvailabilityStatus.RESERVED) {
			slot.reserve();
		}

		if (status == AvailabilityStatus.BOOKED) {
			slot.reserve();
			slot.confirmBooking();
		}

		return slot;
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
		fail("Expected constraint violation: " + expectedConstraintName);
	}

	@Test
	void shouldPersistMinimumBookingNoticeOverrideAsMinutes() {
		User specialistUser = userRepository.saveAndFlush(new User("specialist-notice-persistence@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository.saveAndFlush(
				new Specialist(specialistUser, "Anna", "NoticePersistence", Duration.ofHours(2), Duration.ZERO));

		Instant start = clock.instant().truncatedTo(ChronoUnit.SECONDS).plus(Duration.ofDays(20));

		AvailabilitySlot slot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, start, start.plus(Duration.ofHours(1)), Duration.ofMinutes(30)));

		Integer persistedMinutes = jdbcTemplate.queryForObject("""
				SELECT minimum_booking_notice_minutes
				FROM slots
				WHERE id = ?
				""", Integer.class, slot.getId());

		assertEquals(30, persistedMinutes);

		Long slotId = slot.getId();

		entityManager.clear();

		AvailabilitySlot restored = slotRepository.findById(slotId).orElseThrow();

		assertEquals(Duration.ofMinutes(30), restored.getEffectiveMinimumBookingNotice());
	}

	@Test
	void shouldPersistNullMinimumBookingNoticeOverrideAndUseSpecialistDefault() {
		User specialistUser = userRepository.saveAndFlush(new User("specialist-null-notice@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "NullNotice", Duration.ofHours(2), Duration.ZERO));

		Instant start = clock.instant().truncatedTo(ChronoUnit.SECONDS).plus(Duration.ofDays(21));

		AvailabilitySlot slot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, start, start.plus(Duration.ofHours(1))));

		Integer persistedMinutes = jdbcTemplate.queryForObject("""
				SELECT minimum_booking_notice_minutes
				FROM slots
				WHERE id = ?
				""", Integer.class, slot.getId());

		assertNull(persistedMinutes);

		Long slotId = slot.getId();

		entityManager.clear();

		AvailabilitySlot restored = slotRepository.findById(slotId).orElseThrow();

		assertEquals(Duration.ofHours(2), restored.getEffectiveMinimumBookingNotice());
	}
}
