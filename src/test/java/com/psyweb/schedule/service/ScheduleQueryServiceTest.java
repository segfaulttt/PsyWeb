
package com.psyweb.schedule.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.psyweb.schedule.domain.DateSchedule;
import com.psyweb.schedule.domain.DateScheduleInterval;
import com.psyweb.schedule.domain.DateSchedulePurpose;
import com.psyweb.schedule.domain.ScheduleMode;
import com.psyweb.schedule.domain.SpecialistScheduleSettings;
import com.psyweb.schedule.domain.WeeklyScheduleVersion;
import com.psyweb.schedule.domain.WeeklyWorkingInterval;
import com.psyweb.schedule.repository.DateScheduleRepository;
import com.psyweb.schedule.repository.SpecialistScheduleSettingsRepository;
import com.psyweb.schedule.repository.WeeklyScheduleVersionRepository;
import com.psyweb.schedule.service.model.WorkingInterval;
import com.psyweb.specialist.domain.Specialist;

class ScheduleQueryServiceTest {

	private static final Long SPECIALIST_ID = 1L;

	private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");

	// 10 November 2026 is Tuesday.
	private static final LocalDate DATE = LocalDate.of(2026, 11, 10);

	private SpecialistScheduleSettingsRepository settingsRepository;
	private DateScheduleRepository dateScheduleRepository;
	private WeeklyScheduleVersionRepository weeklyRepository;

	private ScheduleQueryService service;
	private Specialist specialist;

	@BeforeEach
	void setUp() {
		settingsRepository = mock(SpecialistScheduleSettingsRepository.class);

		dateScheduleRepository = mock(DateScheduleRepository.class);

		weeklyRepository = mock(WeeklyScheduleVersionRepository.class);

		specialist = mock(Specialist.class);

		service = new ScheduleQueryService(settingsRepository, dateScheduleRepository, weeklyRepository);
	}

	private void setMode(ScheduleMode mode) {
		SpecialistScheduleSettings settings = new SpecialistScheduleSettings(specialist, NOW);

		settings.changeMode(mode, NOW);

		when(settingsRepository.findById(SPECIALIST_ID)).thenReturn(Optional.of(settings));
	}

	private DateSchedule dateSchedule(DateSchedulePurpose purpose, int startHour, int endHour) {
		DateSchedule schedule = new DateSchedule(specialist, DATE, purpose, NOW);

		schedule.addInterval(new DateScheduleInterval(LocalTime.of(startHour, 0), LocalTime.of(endHour, 0)));

		return schedule;
	}

	private WeeklyScheduleVersion weeklyVersion() {
		return new WeeklyScheduleVersion(specialist, DATE.minusDays(7), null, NOW);
	}

	private WorkingInterval workingInterval(int start, int end) {
		return new WorkingInterval(LocalTime.of(start, 0), LocalTime.of(end, 0));
	}

	@Test
	void shouldReturnScheduleSettings() {
		setMode(ScheduleMode.REGULAR);

		SpecialistScheduleSettings result = service.getScheduleSettings(SPECIALIST_ID);

		assertEquals(ScheduleMode.REGULAR, result.getMode());
		assertEquals(30, result.getBookingHorizonDays());

		verify(settingsRepository).findById(SPECIALIST_ID);
	}

	@Test
	void shouldRejectNullSpecialistId() {
		assertThrows(IllegalArgumentException.class, () -> service.getScheduleSettings(null));

		assertThrows(IllegalArgumentException.class, () -> service.getEffectiveWorkingIntervals(null, DATE));

		verifyNoInteractions(settingsRepository, dateScheduleRepository, weeklyRepository);
	}

	@Test
	void shouldRejectNullDate() {
		assertThrows(IllegalArgumentException.class, () -> service.getEffectiveWorkingIntervals(SPECIALIST_ID, null));

		verifyNoInteractions(settingsRepository, dateScheduleRepository, weeklyRepository);
	}

	@Test
	void shouldThrowWhenSettingsNotFound() {
		when(settingsRepository.findById(SPECIALIST_ID)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class, () -> service.getScheduleSettings(SPECIALIST_ID));
	}

	@Test
	void shouldReturnManualIntervalsInFloatingMode() {
		setMode(ScheduleMode.FLOATING);

		DateSchedule manual = dateSchedule(DateSchedulePurpose.MANUAL, 9, 12);

		manual.addInterval(new DateScheduleInterval(LocalTime.of(14, 0), LocalTime.of(18, 0)));

		when(dateScheduleRepository.findBySpecialistAndDateAndPurpose(SPECIALIST_ID, DATE, DateSchedulePurpose.MANUAL))
				.thenReturn(Optional.of(manual));

		List<WorkingInterval> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE);

		assertEquals(List.of(workingInterval(9, 12), workingInterval(14, 18)), result);

		verifyNoInteractions(weeklyRepository);
	}

	@Test
	void shouldReturnEmptyWhenManualScheduleDoesNotExist() {
		setMode(ScheduleMode.FLOATING);

		when(dateScheduleRepository.findBySpecialistAndDateAndPurpose(SPECIALIST_ID, DATE, DateSchedulePurpose.MANUAL))
				.thenReturn(Optional.empty());

		List<WorkingInterval> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE);

		assertTrue(result.isEmpty());
		verifyNoInteractions(weeklyRepository);
	}

	@Test
	void shouldReturnEmptyForEmptyManualSchedule() {
		setMode(ScheduleMode.FLOATING);

		DateSchedule manual = new DateSchedule(specialist, DATE, DateSchedulePurpose.MANUAL, NOW);

		when(dateScheduleRepository.findBySpecialistAndDateAndPurpose(SPECIALIST_ID, DATE, DateSchedulePurpose.MANUAL))
				.thenReturn(Optional.of(manual));

		List<WorkingInterval> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE);

		assertTrue(result.isEmpty());
	}

	@Test
	void shouldReturnWeeklyIntervalsWhenOverrideDoesNotExist() {
		setMode(ScheduleMode.REGULAR);

		when(dateScheduleRepository.findBySpecialistAndDateAndPurpose(SPECIALIST_ID, DATE,
				DateSchedulePurpose.OVERRIDE)).thenReturn(Optional.empty());

		WeeklyScheduleVersion version = weeklyVersion();

		version.addInterval(new WeeklyWorkingInterval(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));

		version.addInterval(new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)));

		version.addInterval(new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)));

		version.addInterval(new WeeklyWorkingInterval(DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(11, 0)));

		when(weeklyRepository.findEffectiveVersion(SPECIALIST_ID, DATE)).thenReturn(Optional.of(version));

		List<WorkingInterval> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE);

		assertEquals(List.of(workingInterval(10, 12), workingInterval(14, 18)), result);
	}

	@Test
	void shouldReturnOverrideInsteadOfWeeklyIntervals() {
		setMode(ScheduleMode.REGULAR);

		DateSchedule override = dateSchedule(DateSchedulePurpose.OVERRIDE, 15, 19);

		when(dateScheduleRepository.findBySpecialistAndDateAndPurpose(SPECIALIST_ID, DATE,
				DateSchedulePurpose.OVERRIDE)).thenReturn(Optional.of(override));

		List<WorkingInterval> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE);

		assertEquals(List.of(workingInterval(15, 19)), result);

		verifyNoInteractions(weeklyRepository);
	}

	@Test
	void shouldReturnEmptyWhenOverrideClosesDay() {
		setMode(ScheduleMode.REGULAR);

		DateSchedule override = new DateSchedule(specialist, DATE, DateSchedulePurpose.OVERRIDE, NOW);

		when(dateScheduleRepository.findBySpecialistAndDateAndPurpose(SPECIALIST_ID, DATE,
				DateSchedulePurpose.OVERRIDE)).thenReturn(Optional.of(override));

		List<WorkingInterval> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE);

		assertTrue(result.isEmpty());

		verifyNoInteractions(weeklyRepository);
	}

	@Test
	void shouldReturnEmptyWhenWeeklyVersionDoesNotExist() {
		setMode(ScheduleMode.REGULAR);

		when(dateScheduleRepository.findBySpecialistAndDateAndPurpose(SPECIALIST_ID, DATE,
				DateSchedulePurpose.OVERRIDE)).thenReturn(Optional.empty());

		when(weeklyRepository.findEffectiveVersion(SPECIALIST_ID, DATE)).thenReturn(Optional.empty());

		List<WorkingInterval> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE);

		assertTrue(result.isEmpty());
	}

	@Test
	void shouldReturnEmptyWhenWeeklyVersionHasNoIntervalsForDate() {
		setMode(ScheduleMode.REGULAR);

		when(dateScheduleRepository.findBySpecialistAndDateAndPurpose(SPECIALIST_ID, DATE,
				DateSchedulePurpose.OVERRIDE)).thenReturn(Optional.empty());

		WeeklyScheduleVersion version = weeklyVersion();

		version.addInterval(new WeeklyWorkingInterval(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));

		when(weeklyRepository.findEffectiveVersion(SPECIALIST_ID, DATE)).thenReturn(Optional.of(version));

		List<WorkingInterval> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE);

		assertTrue(result.isEmpty());
	}

	@Test
	void shouldReturnSortedManualIntervals() {
		setMode(ScheduleMode.FLOATING);

		DateSchedule manual = dateSchedule(DateSchedulePurpose.MANUAL, 15, 18);

		manual.addInterval(new DateScheduleInterval(LocalTime.of(9, 0), LocalTime.of(12, 0)));

		when(dateScheduleRepository.findBySpecialistAndDateAndPurpose(SPECIALIST_ID, DATE, DateSchedulePurpose.MANUAL))
				.thenReturn(Optional.of(manual));

		List<WorkingInterval> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE);

		assertEquals(List.of(workingInterval(9, 12), workingInterval(15, 18)), result);
	}

	@Test
	void shouldReturnManualIntervalsForFloatingDateRange() {
		setMode(ScheduleMode.FLOATING);

		LocalDate fromDate = DATE;
		LocalDate toDate = DATE.plusDays(2);

		DateSchedule firstDay = dateSchedule(DateSchedulePurpose.MANUAL, 9, 12);

		DateSchedule thirdDay = new DateSchedule(specialist, toDate, DateSchedulePurpose.MANUAL, NOW);
		thirdDay.addInterval(new DateScheduleInterval(LocalTime.of(14, 0), LocalTime.of(18, 0)));

		when(dateScheduleRepository.findBySpecialistAndDateRange(SPECIALIST_ID, fromDate, toDate))
				.thenReturn(List.of(firstDay, thirdDay));

		Map<LocalDate, List<WorkingInterval>> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, fromDate,
				toDate);

		assertEquals(3, result.size());
		assertEquals(List.of(workingInterval(9, 12)), result.get(fromDate));
		assertEquals(List.of(), result.get(fromDate.plusDays(1)));
		assertEquals(List.of(workingInterval(14, 18)), result.get(toDate));

		verifyNoInteractions(weeklyRepository);
	}

	@Test
	void shouldIgnoreOverridesInFloatingMode() {
		setMode(ScheduleMode.FLOATING);

		DateSchedule override = dateSchedule(DateSchedulePurpose.OVERRIDE, 10, 16);

		when(dateScheduleRepository.findBySpecialistAndDateRange(SPECIALIST_ID, DATE, DATE))
				.thenReturn(List.of(override));

		Map<LocalDate, List<WorkingInterval>> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE, DATE);

		assertEquals(Map.of(DATE, List.of()), result);
		verifyNoInteractions(weeklyRepository);
	}

	@Test
	void shouldReturnEmptyIntervalsForFloatingRangeWithoutSchedules() {
		setMode(ScheduleMode.FLOATING);

		LocalDate toDate = DATE.plusDays(2);

		when(dateScheduleRepository.findBySpecialistAndDateRange(SPECIALIST_ID, DATE, toDate)).thenReturn(List.of());

		Map<LocalDate, List<WorkingInterval>> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE,
				toDate);

		assertEquals(3, result.size());

		for (LocalDate date = DATE; !date.isAfter(toDate); date = date.plusDays(1)) {
			assertEquals(List.of(), result.get(date));
		}
	}

	@Test
	void shouldReturnWeeklyIntervalsForRegularDateRange() {
		setMode(ScheduleMode.REGULAR);

		LocalDate toDate = DATE.plusDays(2);
		WeeklyScheduleVersion version = weeklyVersion();

		version.addInterval(new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));

		version.addInterval(new WeeklyWorkingInterval(DayOfWeek.THURSDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)));

		when(weeklyRepository.findOverlappingPeriod(SPECIALIST_ID, DATE, toDate)).thenReturn(List.of(version));

		when(dateScheduleRepository.findBySpecialistAndDateRange(SPECIALIST_ID, DATE, toDate)).thenReturn(List.of());

		Map<LocalDate, List<WorkingInterval>> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE,
				toDate);

		assertEquals(3, result.size());
		assertEquals(List.of(workingInterval(9, 12)), result.get(DATE));
		assertEquals(List.of(), result.get(DATE.plusDays(1)));
		assertEquals(List.of(workingInterval(14, 18)), result.get(toDate));
	}

	@Test
	void shouldSwitchWeeklyVersionWithinDateRange() {
		setMode(ScheduleMode.REGULAR);

		LocalDate fromDate = DATE;
		LocalDate toDate = DATE.plusDays(7);

		WeeklyScheduleVersion oldVersion = new WeeklyScheduleVersion(specialist, DATE.minusDays(7), DATE.plusDays(6),
				NOW);

		oldVersion.addInterval(new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));

		WeeklyScheduleVersion newVersion = new WeeklyScheduleVersion(specialist, DATE.plusDays(7), null, NOW);

		newVersion.addInterval(new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)));

		when(weeklyRepository.findOverlappingPeriod(SPECIALIST_ID, fromDate, toDate))
				.thenReturn(List.of(oldVersion, newVersion));

		when(dateScheduleRepository.findBySpecialistAndDateRange(SPECIALIST_ID, fromDate, toDate))
				.thenReturn(List.of());

		Map<LocalDate, List<WorkingInterval>> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, fromDate,
				toDate);

		assertEquals(8, result.size());

		assertEquals(List.of(workingInterval(9, 12)), result.get(DATE));

		assertEquals(List.of(workingInterval(14, 18)), result.get(DATE.plusDays(7)));
	}

	@Test
	void shouldReplaceWeeklyIntervalsWithOverrideInRange() {
		setMode(ScheduleMode.REGULAR);

		WeeklyScheduleVersion version = weeklyVersion();

		version.addInterval(new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)));

		DateSchedule override = dateSchedule(DateSchedulePurpose.OVERRIDE, 14, 18);

		when(weeklyRepository.findOverlappingPeriod(SPECIALIST_ID, DATE, DATE)).thenReturn(List.of(version));

		when(dateScheduleRepository.findBySpecialistAndDateRange(SPECIALIST_ID, DATE, DATE))
				.thenReturn(List.of(override));

		Map<LocalDate, List<WorkingInterval>> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE, DATE);

		assertEquals(List.of(workingInterval(14, 18)), result.get(DATE));
	}

	@Test
	void shouldCloseDayWithEmptyOverrideInRange() {
		setMode(ScheduleMode.REGULAR);

		WeeklyScheduleVersion version = weeklyVersion();

		version.addInterval(new WeeklyWorkingInterval(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)));

		DateSchedule emptyOverride = new DateSchedule(specialist, DATE, DateSchedulePurpose.OVERRIDE, NOW);

		when(weeklyRepository.findOverlappingPeriod(SPECIALIST_ID, DATE, DATE)).thenReturn(List.of(version));

		when(dateScheduleRepository.findBySpecialistAndDateRange(SPECIALIST_ID, DATE, DATE))
				.thenReturn(List.of(emptyOverride));

		Map<LocalDate, List<WorkingInterval>> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE, DATE);

		assertEquals(Map.of(DATE, List.of()), result);
	}

	@Test
	void shouldReturnEmptyDaysWithoutWeeklyVersions() {
		setMode(ScheduleMode.REGULAR);

		LocalDate toDate = DATE.plusDays(2);

		when(weeklyRepository.findOverlappingPeriod(SPECIALIST_ID, DATE, toDate)).thenReturn(List.of());

		when(dateScheduleRepository.findBySpecialistAndDateRange(SPECIALIST_ID, DATE, toDate)).thenReturn(List.of());

		Map<LocalDate, List<WorkingInterval>> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE,
				toDate);

		assertEquals(3, result.size());

		for (LocalDate date = DATE; !date.isAfter(toDate); date = date.plusDays(1)) {
			assertEquals(List.of(), result.get(date));
		}
	}

	@Test
	void shouldApplyOverrideWithoutWeeklyVersion() {
		setMode(ScheduleMode.REGULAR);

		DateSchedule override = dateSchedule(DateSchedulePurpose.OVERRIDE, 12, 16);

		when(weeklyRepository.findOverlappingPeriod(SPECIALIST_ID, DATE, DATE)).thenReturn(List.of());

		when(dateScheduleRepository.findBySpecialistAndDateRange(SPECIALIST_ID, DATE, DATE))
				.thenReturn(List.of(override));

		Map<LocalDate, List<WorkingInterval>> result = service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE, DATE);

		assertEquals(List.of(workingInterval(12, 16)), result.get(DATE));
	}

	@Test
	void shouldRejectInvalidDateRangeArguments() {
		assertThrows(IllegalArgumentException.class,
				() -> service.getEffectiveWorkingIntervals(null, DATE, DATE.plusDays(1)));

		assertThrows(IllegalArgumentException.class,
				() -> service.getEffectiveWorkingIntervals(SPECIALIST_ID, null, DATE));

		assertThrows(IllegalArgumentException.class,
				() -> service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE, null));

		assertThrows(IllegalArgumentException.class,
				() -> service.getEffectiveWorkingIntervals(SPECIALIST_ID, DATE.plusDays(1), DATE));

		verifyNoInteractions(settingsRepository, dateScheduleRepository, weeklyRepository);
	}

}
