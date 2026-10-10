package com.psyweb.availability.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.exception.SlotOverlapException;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

public class AvailabilitySlotServiceIntegrationTest extends PostgreSQLIntegrationTest {

	@Autowired
	private Clock clock;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SpecialistRepository specialistRepository;

	@MockitoSpyBean
	private AvailabilitySlotRepository slotRepository;

	@Autowired
	private AvailabilitySlotService slotService;

	@Test
	void shouldTranslateDatabaseOverlapViolationToSlotOverlapException() {
		User user = userRepository.saveAndFlush(
				new User("specialist-db-overlap@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = new Specialist(user, "Anna", "DbOverlap", Duration.ZERO, Duration.ZERO);

		specialist.approve();
		specialistRepository.saveAndFlush(specialist);

		Instant start = clock.instant().truncatedTo(ChronoUnit.SECONDS).plus(Duration.ofDays(20));

		slotRepository.saveAndFlush(new AvailabilitySlot(specialist, start, start.plus(Duration.ofHours(1))));

		doReturn(false).when(slotRepository).existsOverlappingSlot(specialist.getId(), start.plus(Duration.ofMinutes(30)),
				start.plus(Duration.ofHours(1)).plus(Duration.ofMinutes(30)));

		SlotOverlapException exception = assertThrows(SlotOverlapException.class, () -> slotService
				.createSlot(specialist.getId(), start.plus(Duration.ofMinutes(30)), start.plus(Duration.ofHours(1)).plus(Duration.ofMinutes(30))));

		assertEquals("SLOT_OVERLAP", exception.code());
		assertEquals("Slot overlap", exception.getMessage());
		assertInstanceOf(DataIntegrityViolationException.class, exception.getCause());
	}
}