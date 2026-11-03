package com.psyweb.schedule.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.psyweb.schedule.domain.*;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

import jakarta.persistence.EntityManager;

class ScheduleRepositoryIntegrationTest extends PostgreSQLIntegrationTest {

	private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");

	private static final LocalDate DATE = LocalDate.of(2026, 11, 10);

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private SpecialistScheduleSettingsRepository settingsRepository;

	@Autowired
	private SpecialistTimeZoneGuardRepository timeZoneRepository;

	@Autowired
	private WeeklyScheduleVersionRepository weeklyRepository;

	@Autowired
	private DateScheduleRepository dateRepository;

	private <T> T tx(Supplier<T> action) {
		return new TransactionTemplate(transactionManager).execute(status -> action.get());
	}

	private Specialist createSpecialist() {
		User user = new User("schedule-repo-" + UUID.randomUUID() + "@example.com", "password-hash",
				UserRole.SPECIALIST, UserStatus.ACTIVE);

		entityManager.persist(user);
		entityManager.flush();

		Specialist specialist = new Specialist(user, "Anna", "Schedule", Duration.ZERO, Duration.ZERO);

		entityManager.persist(specialist);
		entityManager.flush();

		return specialist;
	}

	@Test
	void shouldFindEffectiveWeeklyVersionAtBoundaries() {
		Long specialistId = tx(() -> {
			Specialist specialist = createSpecialist();

			WeeklyScheduleVersion version = new WeeklyScheduleVersion(specialist, DATE, DATE.plusDays(7), NOW);

			version.addInterval(new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));

			weeklyRepository.saveAndFlush(version);
			return specialist.getId();
		});

		tx(() -> {
			assertTrue(weeklyRepository.findEffectiveVersion(specialistId, DATE.minusDays(1)).isEmpty());

			assertTrue(weeklyRepository.findEffectiveVersion(specialistId, DATE).isPresent());

			assertTrue(weeklyRepository.findEffectiveVersion(specialistId, DATE.plusDays(7)).isPresent());

			assertTrue(weeklyRepository.findEffectiveVersion(specialistId, DATE.plusDays(8)).isEmpty());

			return null;
		});
	}

	@Test
	void shouldFindWeeklyVersionsOverlappingPeriod() {
		Long specialistId = tx(() -> {
			Specialist specialist = createSpecialist();

			weeklyRepository.save(new WeeklyScheduleVersion(specialist, DATE, DATE.plusDays(6), NOW));

			weeklyRepository.save(new WeeklyScheduleVersion(specialist, DATE.plusDays(7), DATE.plusDays(13), NOW));

			weeklyRepository.save(new WeeklyScheduleVersion(specialist, DATE.plusDays(14), null, NOW));

			return specialist.getId();
		});

		tx(() -> {
			List<WeeklyScheduleVersion> result = weeklyRepository.findOverlappingPeriod(specialistId, DATE.plusDays(5),
					DATE.plusDays(9));

			assertEquals(2, result.size());
			assertEquals(DATE, result.get(0).getEffectiveFrom());
			assertEquals(DATE.plusDays(7), result.get(1).getEffectiveFrom());

			return null;
		});
	}

	@Test
	void shouldFindDateScheduleByPurpose() {
		Long specialistId = tx(() -> {
			Specialist specialist = createSpecialist();

			DateSchedule manual = new DateSchedule(specialist, DATE, DateSchedulePurpose.MANUAL, NOW);

			manual.addInterval(new DateScheduleInterval(LocalTime.of(9, 0), LocalTime.of(12, 0)));

			DateSchedule override = new DateSchedule(specialist, DATE, DateSchedulePurpose.OVERRIDE, NOW);

			dateRepository.save(manual);
			dateRepository.save(override);

			return specialist.getId();
		});

		tx(() -> {
			DateSchedule manual = dateRepository
					.findBySpecialistAndDateAndPurpose(specialistId, DATE, DateSchedulePurpose.MANUAL).orElseThrow();

			DateSchedule override = dateRepository
					.findBySpecialistAndDateAndPurpose(specialistId, DATE, DateSchedulePurpose.OVERRIDE).orElseThrow();

			assertEquals(DateSchedulePurpose.MANUAL, manual.getPurpose());
			assertEquals(1, manual.getIntervals().size());

			assertEquals(DateSchedulePurpose.OVERRIDE, override.getPurpose());
			assertTrue(override.isClosed());

			return null;
		});
	}

	@Test
	void shouldFindDateSchedulesWithinRange() {
		Long specialistId = tx(() -> {
			Specialist specialist = createSpecialist();

			for (int offset : new int[] { 0, 3, 7, 15 }) {
				dateRepository
						.save(new DateSchedule(specialist, DATE.plusDays(offset), DateSchedulePurpose.MANUAL, NOW));
			}

			return specialist.getId();
		});

		tx(() -> {
			List<DateSchedule> result = dateRepository.findBySpecialistAndDateRange(specialistId, DATE.plusDays(2),
					DATE.plusDays(10));

			assertEquals(2, result.size());
			assertEquals(DATE.plusDays(3), result.get(0).getScheduleDate());
			assertEquals(DATE.plusDays(7), result.get(1).getScheduleDate());

			return null;
		});
	}

	@Test
	void shouldReadScheduleSettingsWithWriteLock() {
		Long specialistId = tx(() -> {
			Specialist specialist = createSpecialist();

			settingsRepository.save(new SpecialistScheduleSettings(specialist, NOW));

			return specialist.getId();
		});

		tx(() -> {
			SpecialistScheduleSettings settings = settingsRepository.findForUpdateBySpecialistId(specialistId)
					.orElseThrow();

			assertEquals(ScheduleMode.REGULAR, settings.getMode());
			assertEquals(30, settings.getBookingHorizonDays());

			return null;
		});
	}

	@Test
	void shouldReadTimeZoneWithBothLockModes() {
		Long specialistId = tx(() -> {
			Specialist specialist = createSpecialist();

			timeZoneRepository.save(new SpecialistTimeZoneGuard(specialist, ZoneId.of("Europe/Amsterdam"), NOW));

			return specialist.getId();
		});

		tx(() -> {
			SpecialistTimeZoneGuard guard = timeZoneRepository.findForShareBySpecialistId(specialistId).orElseThrow();

			assertEquals(ZoneId.of("Europe/Amsterdam"), guard.getTimeZone());

			return null;
		});

		tx(() -> {
			SpecialistTimeZoneGuard guard = timeZoneRepository.findForUpdateBySpecialistId(specialistId).orElseThrow();

			assertEquals(ZoneId.of("Europe/Amsterdam"), guard.getTimeZone());

			return null;
		});
	}
}