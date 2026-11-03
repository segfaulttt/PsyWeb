
package com.psyweb.schedule.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.psyweb.specialist.domain.Specialist;

class ScheduleDomainTest {

	private static final Instant CREATED = Instant.parse("2026-11-03T10:00:00Z");

	private static final Instant UPDATED = Instant.parse("2026-11-03T11:00:00Z");

	private Specialist specialist() {
		return mock(Specialist.class);
	}

	private DateScheduleInterval interval(int start, int end) {
		return new DateScheduleInterval(LocalTime.of(start, 0), LocalTime.of(end, 0));
	}

	@Test
	void shouldRejectOverlappingDateIntervals() {
		DateSchedule schedule = new DateSchedule(specialist(), LocalDate.of(2026, 11, 10), DateSchedulePurpose.OVERRIDE,
				CREATED);

		schedule.addInterval(interval(9, 12));

		assertThrows(IllegalArgumentException.class, () -> schedule.addInterval(interval(11, 14)));

		assertEquals(1, schedule.getIntervals().size());
	}

	@Test
	void shouldAllowAdjacentDateIntervals() {
		DateSchedule schedule = new DateSchedule(specialist(), LocalDate.of(2026, 11, 10), DateSchedulePurpose.MANUAL,
				CREATED);

		schedule.addInterval(interval(9, 12));
		schedule.addInterval(interval(12, 15));

		assertEquals(2, schedule.getIntervals().size());
	}

	@Test
	void shouldPreserveExistingIntervalWhenTimeRangeIsUnchanged() {
		DateSchedule schedule = new DateSchedule(specialist(), LocalDate.of(2026, 11, 10), DateSchedulePurpose.OVERRIDE,
				CREATED);

		DateScheduleInterval original = interval(9, 12);
		schedule.addInterval(original);

		schedule.replaceIntervals(List.of(interval(9, 12)), UPDATED);

		assertEquals(1, schedule.getIntervals().size());
		assertSame(original, schedule.getIntervals().get(0));
		assertEquals(CREATED, schedule.getUpdatedAt());
	}

	@Test
	void shouldReplaceOnlyChangedIntervals() {
		DateSchedule schedule = new DateSchedule(specialist(), LocalDate.of(2026, 11, 10), DateSchedulePurpose.OVERRIDE,
				CREATED);

		DateScheduleInterval retained = interval(9, 12);
		schedule.addInterval(retained);
		schedule.addInterval(interval(14, 18));

		schedule.replaceIntervals(List.of(interval(9, 12), interval(15, 19)), UPDATED);

		assertEquals(2, schedule.getIntervals().size());
		assertSame(retained, schedule.getIntervals().get(0));
		assertEquals(LocalTime.of(15, 0), schedule.getIntervals().get(1).getStartTime());
		assertEquals(UPDATED, schedule.getUpdatedAt());
	}

	@Test
	void shouldCloseDateScheduleAndRemainIdempotent() {
		DateSchedule schedule = new DateSchedule(specialist(), LocalDate.of(2026, 11, 10), DateSchedulePurpose.OVERRIDE,
				CREATED);

		schedule.addInterval(interval(9, 12));

		schedule.clearIntervals(UPDATED);

		assertTrue(schedule.isClosed());
		assertEquals(UPDATED, schedule.getUpdatedAt());

		schedule.clearIntervals(UPDATED.plusSeconds(60));

		assertTrue(schedule.isClosed());
		assertEquals(UPDATED, schedule.getUpdatedAt());
	}

	@Test
	void shouldRejectIntervalOwnedByAnotherDateSchedule() {
		Specialist specialist = specialist();

		DateSchedule first = new DateSchedule(specialist, LocalDate.of(2026, 11, 10), DateSchedulePurpose.MANUAL,
				CREATED);

		DateSchedule second = new DateSchedule(specialist, LocalDate.of(2026, 11, 11), DateSchedulePurpose.MANUAL,
				CREATED);

		DateScheduleInterval existing = interval(9, 12);
		first.addInterval(existing);

		assertThrows(IllegalStateException.class, () -> second.addInterval(existing));

		assertEquals(1, first.getIntervals().size());
		assertTrue(second.getIntervals().isEmpty());
	}

	@Test
	void shouldRejectOverlappingWeeklyIntervalsOnSameDay() {
		WeeklyScheduleVersion version = new WeeklyScheduleVersion(specialist(), LocalDate.of(2026, 11, 1), null,
				CREATED);

		version.addInterval(new WeeklyWorkingInterval(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));

		assertThrows(IllegalArgumentException.class, () -> version
				.addInterval(new WeeklyWorkingInterval(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(14, 0))));

		assertEquals(1, version.getIntervals().size());
	}

	@Test
	void shouldAllowSameWeeklyHoursOnDifferentDays() {
		WeeklyScheduleVersion version = new WeeklyScheduleVersion(specialist(), LocalDate.of(2026, 11, 1), null,
				CREATED);

		version.addInterval(new WeeklyWorkingInterval(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));

		version.addInterval(new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));

		assertEquals(2, version.getIntervals().size());
	}

	@Test
	void shouldChangeTimeZoneAndIgnoreRepeatedValue() {
		SpecialistTimeZoneGuard guard = new SpecialistTimeZoneGuard(specialist(), ZoneId.of("Europe/Amsterdam"),
				CREATED);

		guard.changeTimeZone(ZoneId.of("Europe/Berlin"), UPDATED);

		assertEquals(ZoneId.of("Europe/Berlin"), guard.getTimeZone());
		assertEquals(UPDATED, guard.getUpdatedAt());

		guard.changeTimeZone(ZoneId.of("Europe/Berlin"), UPDATED.plusSeconds(60));

		assertEquals(UPDATED, guard.getUpdatedAt());
	}

	@Test
	void shouldRejectEarlierTimeZoneUpdate() {
		SpecialistTimeZoneGuard guard = new SpecialistTimeZoneGuard(specialist(), ZoneId.of("UTC"), CREATED);

		assertThrows(IllegalArgumentException.class,
				() -> guard.changeTimeZone(ZoneId.of("Europe/Amsterdam"), CREATED.minusSeconds(1)));

		assertEquals(ZoneId.of("UTC"), guard.getTimeZone());
	}
}
