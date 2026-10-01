package com.psyweb.availability.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

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
		LocalDateTime now = LocalDateTime.now(clock).withNano(0);
		LocalDateTime startTime = now.plusDays(1);
		LocalDateTime cancelledAt = now.plusMinutes(5);

		User specialistUser = userRepository.saveAndFlush(new User("specialist-slot-metadata@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "SlotMetadata", Duration.ZERO, Duration.ZERO));

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, startTime.plusHours(1));

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
		LocalDateTime now = LocalDateTime.now(clock).withNano(0);
		LocalDateTime startTime = now.plusDays(1);
		LocalDateTime cancelledAt = now.plusMinutes(5);

		User specialistUser = userRepository.saveAndFlush(new User("specialist-slot-partial@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "SlotPartial", Duration.ZERO, Duration.ZERO));

		AvailabilitySlot slot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, startTime, startTime.plusHours(1)));

		Long slotId = slot.getId();

		assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
				UPDATE slots
				SET cancelled_at = ?
				WHERE id = ?
				""", cancelledAt, slotId));
	}

	@Test
	void shouldAllowCreatingNewSlotForCancelledTime() {
		LocalDateTime now = LocalDateTime.now(clock).withNano(0);
		LocalDateTime startTime = now.plusDays(1);
		LocalDateTime endTime = startTime.plusHours(1);

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
}
