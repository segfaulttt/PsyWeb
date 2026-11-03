package com.psyweb.schedule.persistence;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.psyweb.schedule.domain.DateSchedule;
import com.psyweb.schedule.domain.DateScheduleInterval;
import com.psyweb.schedule.domain.DateSchedulePurpose;
import com.psyweb.schedule.domain.ScheduleMode;
import com.psyweb.schedule.domain.SpecialistScheduleSettings;
import com.psyweb.schedule.domain.SpecialistTimeZoneGuard;
import com.psyweb.schedule.domain.WeeklyScheduleVersion;
import com.psyweb.schedule.domain.WeeklyWorkingInterval;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

import jakarta.persistence.EntityManager;

class SchedulePersistenceIntegrationTest extends PostgreSQLIntegrationTest {

	private static final Instant CREATED = Instant.parse("2026-11-03T10:00:00Z");

	private static final Instant UPDATED = Instant.parse("2026-11-03T11:00:00Z");

	private static final LocalDate DATE = LocalDate.of(2026, 11, 10);

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private <T> T inTransaction(Supplier<T> action) {
		return new TransactionTemplate(transactionManager).execute(status -> action.get());
	}

	private Specialist persistSpecialist() {
		User user = new User("schedule-" + UUID.randomUUID() + "@example.com", "password-hash", UserRole.SPECIALIST,
				UserStatus.ACTIVE);

		entityManager.persist(user);
		entityManager.flush();

		Specialist specialist = new Specialist(user, "Anna", "Schedule", Duration.ZERO, Duration.ZERO);

		entityManager.persist(specialist);
		entityManager.flush();

		return specialist;
	}

	private DateScheduleInterval interval(int start, int end) {
		return new DateScheduleInterval(LocalTime.of(start, 0), LocalTime.of(end, 0));
	}

	@Test
	void shouldPersistScheduleSettingsAndTimeZoneGuard() {
		Long specialistId = inTransaction(() -> {
			Specialist specialist = persistSpecialist();

			SpecialistScheduleSettings settings = new SpecialistScheduleSettings(specialist, CREATED);

			SpecialistTimeZoneGuard guard = new SpecialistTimeZoneGuard(specialist, ZoneId.of("Europe/Amsterdam"),
					CREATED);

			entityManager.persist(settings);
			entityManager.persist(guard);
			entityManager.flush();

			return specialist.getId();
		});

		inTransaction(() -> {
			SpecialistScheduleSettings settings = entityManager.find(SpecialistScheduleSettings.class, specialistId);

			SpecialistTimeZoneGuard guard = entityManager.find(SpecialistTimeZoneGuard.class, specialistId);

			assertNotNull(settings);
			assertNotNull(guard);

			assertEquals(specialistId, settings.getSpecialistId());
			assertEquals(ScheduleMode.REGULAR, settings.getMode());
			assertEquals(30, settings.getBookingHorizonDays());

			assertEquals(specialistId, guard.getSpecialistId());
			assertEquals(ZoneId.of("Europe/Amsterdam"), guard.getTimeZone());
			assertEquals(CREATED, guard.getUpdatedAt());

			return null;
		});
	}

	@Test
	void shouldPersistWeeklyScheduleWithIntervals() {
		UUID versionId = inTransaction(() -> {
			Specialist specialist = persistSpecialist();

			WeeklyScheduleVersion version = new WeeklyScheduleVersion(specialist, DATE, null, CREATED);

			version.addInterval(new WeeklyWorkingInterval(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));

			version.addInterval(new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)));

			entityManager.persist(version);
			entityManager.flush();

			assertNotNull(version.getId());

			for (WeeklyWorkingInterval interval : version.getIntervals()) {
				assertNotNull(interval.getId());
			}

			return version.getId();
		});

		inTransaction(() -> {
			WeeklyScheduleVersion restored = entityManager.find(WeeklyScheduleVersion.class, versionId);

			assertNotNull(restored);
			assertEquals(DATE, restored.getEffectiveFrom());
			assertNull(restored.getEffectiveUntil());
			assertEquals(2, restored.getIntervals().size());

			assertTrue(restored.getIntervals().stream().anyMatch(i -> i.getDayOfWeek() == DayOfWeek.MONDAY));

			assertTrue(restored.getIntervals().stream().anyMatch(i -> i.getDayOfWeek() == DayOfWeek.TUESDAY));

			return null;
		});
	}

	@Test
	void shouldPersistAndRestoreDateSchedule() {
		UUID scheduleId = inTransaction(() -> {
			Specialist specialist = persistSpecialist();

			DateSchedule schedule = new DateSchedule(specialist, DATE, DateSchedulePurpose.OVERRIDE, CREATED);

			schedule.addInterval(interval(9, 12));
			schedule.addInterval(interval(14, 18));

			entityManager.persist(schedule);
			entityManager.flush();

			assertNotNull(schedule.getId());

			return schedule.getId();
		});

		inTransaction(() -> {
			DateSchedule restored = entityManager.find(DateSchedule.class, scheduleId);

			assertNotNull(restored);
			assertEquals(DATE, restored.getScheduleDate());
			assertEquals(DateSchedulePurpose.OVERRIDE, restored.getPurpose());
			assertEquals(2, restored.getIntervals().size());
			assertFalse(restored.isClosed());

			return null;
		});
	}

	@Test
	void shouldReplaceDateIntervalsWithoutDeletingUnchangedInterval() {
		UUID scheduleId = inTransaction(() -> {
			Specialist specialist = persistSpecialist();

			DateSchedule schedule = new DateSchedule(specialist, DATE, DateSchedulePurpose.OVERRIDE, CREATED);

			schedule.addInterval(interval(9, 12));
			schedule.addInterval(interval(14, 18));

			entityManager.persist(schedule);
			entityManager.flush();

			return schedule.getId();
		});

		UUID retainedIntervalId = inTransaction(() -> {
			DateSchedule schedule = entityManager.find(DateSchedule.class, scheduleId);

			UUID retainedId = schedule.getIntervals().stream().filter(i -> i.getStartTime().equals(LocalTime.of(9, 0)))
					.findFirst().orElseThrow().getId();

			schedule.replaceIntervals(List.of(interval(9, 12), interval(15, 19)), UPDATED);

			entityManager.flush();

			return retainedId;
		});

		inTransaction(() -> {
			DateSchedule restored = entityManager.find(DateSchedule.class, scheduleId);

			assertEquals(2, restored.getIntervals().size());
			assertEquals(UPDATED, restored.getUpdatedAt());

			DateScheduleInterval retained = restored.getIntervals().stream()
					.filter(i -> i.getStartTime().equals(LocalTime.of(9, 0))).findFirst().orElseThrow();

			assertEquals(retainedIntervalId, retained.getId());

			assertTrue(restored.getIntervals().stream().anyMatch(
					i -> i.getStartTime().equals(LocalTime.of(15, 0)) && i.getEndTime().equals(LocalTime.of(19, 0))));

			assertFalse(restored.getIntervals().stream().anyMatch(i -> i.getStartTime().equals(LocalTime.of(14, 0))));

			return null;
		});
	}

	@Test
	void shouldPersistEmptyOverrideAsClosedDay() {
		UUID scheduleId = inTransaction(() -> {
			Specialist specialist = persistSpecialist();

			DateSchedule schedule = new DateSchedule(specialist, DATE, DateSchedulePurpose.OVERRIDE, CREATED);

			entityManager.persist(schedule);
			return schedule.getId();
		});

		inTransaction(() -> {
			DateSchedule restored = entityManager.find(DateSchedule.class, scheduleId);

			assertNotNull(restored);
			assertTrue(restored.isClosed());
			assertTrue(restored.getIntervals().isEmpty());

			return null;
		});
	}

	@Test
	void shouldRejectDuplicateDateSchedule() {
		assertThrows(RuntimeException.class, () -> inTransaction(() -> {
			Specialist specialist = persistSpecialist();

			DateSchedule first = new DateSchedule(specialist, DATE, DateSchedulePurpose.OVERRIDE, CREATED);

			DateSchedule duplicate = new DateSchedule(specialist, DATE, DateSchedulePurpose.OVERRIDE, CREATED);

			entityManager.persist(first);
			entityManager.persist(duplicate);
			entityManager.flush();

			return null;
		}));
	}

	@Test
	void shouldRejectOverlappingWeeklyVersionPeriods() {
		assertThrows(RuntimeException.class, () -> inTransaction(() -> {
			Specialist specialist = persistSpecialist();

			WeeklyScheduleVersion first = new WeeklyScheduleVersion(specialist, DATE, DATE.plusDays(10), CREATED);

			WeeklyScheduleVersion second = new WeeklyScheduleVersion(specialist, DATE.plusDays(5), DATE.plusDays(20),
					CREATED);

			entityManager.persist(first);
			entityManager.persist(second);

			return null;
		}));
	}

	@Test
	void shouldRejectOverlappingDateIntervalsAtCommit() {
		assertThrows(RuntimeException.class, () -> inTransaction(() -> {
			Specialist specialist = persistSpecialist();

			DateSchedule schedule = new DateSchedule(specialist, DATE, DateSchedulePurpose.OVERRIDE, CREATED);

			entityManager.persist(schedule);
			entityManager.flush();

			jdbcTemplate.update("""
					INSERT INTO date_schedule_intervals
					    (id, date_schedule_id, start_time, end_time)
					VALUES (?, ?, TIME '09:00', TIME '12:00')
					""", UUID.randomUUID(), schedule.getId());

			jdbcTemplate.update("""
					INSERT INTO date_schedule_intervals
					    (id, date_schedule_id, start_time, end_time)
					VALUES (?, ?, TIME '11:00', TIME '14:00')
					""", UUID.randomUUID(), schedule.getId());

			return null;
		}));
	}

	@Test
	void shouldRejectOverlappingWeeklyIntervalsAtCommit() {
		assertThrows(RuntimeException.class, () -> inTransaction(() -> {
			Specialist specialist = persistSpecialist();

			WeeklyScheduleVersion version = new WeeklyScheduleVersion(specialist, DATE, null, CREATED);

			entityManager.persist(version);
			entityManager.flush();

			jdbcTemplate.update("""
					INSERT INTO weekly_working_intervals
					    (id, schedule_version_id, day_of_week,
					     start_time, end_time)
					VALUES (?, ?, 1, TIME '09:00', TIME '12:00')
					""", UUID.randomUUID(), version.getId());

			jdbcTemplate.update("""
					INSERT INTO weekly_working_intervals
					    (id, schedule_version_id, day_of_week,
					     start_time, end_time)
					VALUES (?, ?, 1, TIME '11:00', TIME '14:00')
					""", UUID.randomUUID(), version.getId());

			return null;
		}));
	}
}