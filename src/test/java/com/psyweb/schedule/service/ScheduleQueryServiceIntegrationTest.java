package com.psyweb.schedule.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.psyweb.schedule.domain.*;
import com.psyweb.schedule.repository.*;
import com.psyweb.schedule.service.model.WorkingInterval;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

import jakarta.persistence.EntityManager;

class ScheduleQueryServiceIntegrationTest extends PostgreSQLIntegrationTest {

	private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");

	private static final LocalDate DATE = LocalDate.of(2026, 11, 10);

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private ScheduleQueryService scheduleQueryService;

	@Autowired
	private SpecialistScheduleSettingsRepository settingsRepository;

	@Autowired
	private WeeklyScheduleVersionRepository weeklyRepository;

	@Autowired
	private DateScheduleRepository dateRepository;

	private <T> T tx(Supplier<T> action) {
		return new TransactionTemplate(transactionManager).execute(status -> action.get());
	}

	private Specialist createSpecialist(ScheduleMode mode) {
		User user = new User("schedule-query-" + UUID.randomUUID() + "@example.com", "password-hash",
				UserRole.SPECIALIST, UserStatus.ACTIVE);

		entityManager.persist(user);
		entityManager.flush();

		Specialist specialist = new Specialist(user, "Anna", "Schedule", Duration.ZERO, Duration.ZERO);

		entityManager.persist(specialist);
		entityManager.flush();

		SpecialistScheduleSettings settings = new SpecialistScheduleSettings(specialist, NOW);

		settings.changeMode(mode, NOW);
		settingsRepository.saveAndFlush(settings);

		return specialist;
	}

	private WorkingInterval interval(int start, int end) {
		return new WorkingInterval(LocalTime.of(start, 0), LocalTime.of(end, 0));
	}

	@Test
	void shouldReadRegularScheduleOutsideSetupTransaction() {
		Long specialistId = tx(() -> {
			Specialist specialist = createSpecialist(ScheduleMode.REGULAR);

			WeeklyScheduleVersion version = new WeeklyScheduleVersion(specialist, DATE.minusDays(7), null, NOW);

			version.addInterval(new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)));

			version.addInterval(new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));

			version.addInterval(
					new WeeklyWorkingInterval(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(16, 0)));

			weeklyRepository.saveAndFlush(version);

			return specialist.getId();
		});

		// Called outside the setup transaction.
		List<WorkingInterval> result = scheduleQueryService.getEffectiveWorkingIntervals(specialistId, DATE);

		assertEquals(List.of(interval(9, 12), interval(14, 18)), result);
	}

	@Test
	void shouldApplyOverrideInsteadOfWeeklySchedule() {
		Long specialistId = tx(() -> {
			Specialist specialist = createSpecialist(ScheduleMode.REGULAR);

			WeeklyScheduleVersion version = new WeeklyScheduleVersion(specialist, DATE, null, NOW);

			version.addInterval(new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)));

			weeklyRepository.save(version);

			DateSchedule override = new DateSchedule(specialist, DATE, DateSchedulePurpose.OVERRIDE, NOW);

			override.addInterval(new DateScheduleInterval(LocalTime.of(14, 0), LocalTime.of(18, 0)));

			dateRepository.save(override);

			return specialist.getId();
		});

		List<WorkingInterval> result = scheduleQueryService.getEffectiveWorkingIntervals(specialistId, DATE);

		assertEquals(List.of(interval(14, 18)), result);
	}

	@Test
	void shouldCloseRegularDayWithEmptyOverride() {
		Long specialistId = tx(() -> {
			Specialist specialist = createSpecialist(ScheduleMode.REGULAR);

			WeeklyScheduleVersion version = new WeeklyScheduleVersion(specialist, DATE, null, NOW);

			version.addInterval(new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)));

			weeklyRepository.save(version);

			// Empty OVERRIDE means a day off.
			DateSchedule override = new DateSchedule(specialist, DATE, DateSchedulePurpose.OVERRIDE, NOW);

			dateRepository.save(override);

			return specialist.getId();
		});

		List<WorkingInterval> result = scheduleQueryService.getEffectiveWorkingIntervals(specialistId, DATE);

		assertTrue(result.isEmpty());
	}

	@Test
	void shouldReadFloatingManualScheduleAcrossDateRange() {
		Long specialistId = tx(() -> {
			Specialist specialist = createSpecialist(ScheduleMode.FLOATING);

			DateSchedule first = new DateSchedule(specialist, DATE, DateSchedulePurpose.MANUAL, NOW);

			first.addInterval(new DateScheduleInterval(LocalTime.of(9, 0), LocalTime.of(12, 0)));

			DateSchedule third = new DateSchedule(specialist, DATE.plusDays(2), DateSchedulePurpose.MANUAL, NOW);

			third.addInterval(new DateScheduleInterval(LocalTime.of(14, 0), LocalTime.of(18, 0)));

			dateRepository.save(first);
			dateRepository.save(third);

			return specialist.getId();
		});

		Map<LocalDate, List<WorkingInterval>> result = scheduleQueryService.getEffectiveWorkingIntervals(specialistId,
				DATE, DATE.plusDays(2));

		assertEquals(3, result.size());

		assertEquals(List.of(interval(9, 12)), result.get(DATE));

		assertEquals(List.of(), result.get(DATE.plusDays(1)));

		assertEquals(List.of(interval(14, 18)), result.get(DATE.plusDays(2)));
	}

	@Test
	void shouldSwitchWeeklyVersionsWithinDateRange() {
		Long specialistId = tx(() -> {
			Specialist specialist = createSpecialist(ScheduleMode.REGULAR);

			WeeklyScheduleVersion oldVersion = new WeeklyScheduleVersion(specialist, DATE, DATE.plusDays(6), NOW);

			oldVersion
					.addInterval(new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));

			WeeklyScheduleVersion newVersion = new WeeklyScheduleVersion(specialist, DATE.plusDays(7), null, NOW);

			newVersion.addInterval(
					new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)));

			weeklyRepository.save(oldVersion);
			weeklyRepository.save(newVersion);

			return specialist.getId();
		});

		Map<LocalDate, List<WorkingInterval>> result = scheduleQueryService.getEffectiveWorkingIntervals(specialistId,
				DATE, DATE.plusDays(7));

		assertEquals(8, result.size());

		assertEquals(List.of(interval(9, 12)), result.get(DATE));

		assertEquals(List.of(interval(14, 18)), result.get(DATE.plusDays(7)));

		assertEquals(List.of(), result.get(DATE.plusDays(1)));
	}

	@Test
	void shouldApplyOverrideWithinRegularDateRange() {
		Long specialistId = tx(() -> {
			Specialist specialist = createSpecialist(ScheduleMode.REGULAR);

			WeeklyScheduleVersion version = new WeeklyScheduleVersion(specialist, DATE, null, NOW);

			version.addInterval(new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)));

			version.addInterval(
					new WeeklyWorkingInterval(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(16, 0)));

			weeklyRepository.save(version);

			DateSchedule override = new DateSchedule(specialist, DATE, DateSchedulePurpose.OVERRIDE, NOW);

			override.addInterval(new DateScheduleInterval(LocalTime.of(13, 0), LocalTime.of(18, 0)));

			dateRepository.save(override);

			return specialist.getId();
		});

		Map<LocalDate, List<WorkingInterval>> result = scheduleQueryService.getEffectiveWorkingIntervals(specialistId,
				DATE, DATE.plusDays(1));

		assertEquals(List.of(interval(13, 18)), result.get(DATE));

		assertEquals(List.of(interval(10, 16)), result.get(DATE.plusDays(1)));
	}

	@Test
	void shouldReturnEmptyIntervalsWhenNoWeeklyVersionExists() {
		Long specialistId = tx(() -> {
			Specialist specialist = createSpecialist(ScheduleMode.REGULAR);

			return specialist.getId();
		});

		Map<LocalDate, List<WorkingInterval>> result = scheduleQueryService.getEffectiveWorkingIntervals(specialistId,
				DATE, DATE.plusDays(2));

		assertEquals(3, result.size());

		for (LocalDate date = DATE; !date.isAfter(DATE.plusDays(2)); date = date.plusDays(1)) {

			assertEquals(List.of(), result.get(date));
		}
	}
}