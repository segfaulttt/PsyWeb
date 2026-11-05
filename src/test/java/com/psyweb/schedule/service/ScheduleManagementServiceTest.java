package com.psyweb.schedule.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.psyweb.schedule.domain.DateSchedule;
import com.psyweb.schedule.domain.DateSchedulePurpose;
import com.psyweb.schedule.domain.ScheduleMode;
import com.psyweb.schedule.domain.SpecialistScheduleSettings;
import com.psyweb.schedule.domain.SpecialistTimeZoneGuard;
import com.psyweb.schedule.domain.WeeklyScheduleVersion;
import com.psyweb.schedule.repository.DateScheduleRepository;
import com.psyweb.schedule.repository.SpecialistScheduleSettingsRepository;
import com.psyweb.schedule.repository.SpecialistTimeZoneGuardRepository;
import com.psyweb.schedule.repository.WeeklyScheduleVersionRepository;
import com.psyweb.schedule.service.model.CreateWeeklyScheduleVersionCommand;
import com.psyweb.schedule.service.model.WeeklyWorkingIntervalCommand;
import com.psyweb.schedule.service.model.WorkingInterval;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.service.SpecialistService;

@ExtendWith(MockitoExtension.class)
class ScheduleManagementServiceTest {

	private static final Long SPECIALIST_ID = 1L;
	private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 5);

	@Mock
	private SpecialistScheduleSettingsRepository settingsRepository;

	@Mock
	private WeeklyScheduleVersionRepository weeklyRepository;

	@Mock
	private DateScheduleRepository dateRepository;

	@Mock
	private SpecialistTimeZoneGuardRepository guardRepository;

	@Mock
	private SpecialistService specialistService;

	@Mock
	private Specialist specialist;

	private ScheduleManagementService service;
	private SpecialistScheduleSettings settings;
	private SpecialistTimeZoneGuard guard;

	@BeforeEach
	void setUp() {
		service = createService(Clock.fixed(NOW, ZoneOffset.UTC));

		settings = new SpecialistScheduleSettings(specialist, NOW.minusSeconds(3600));

		guard = new SpecialistTimeZoneGuard(specialist, ZoneOffset.UTC, NOW.minusSeconds(3600));

		lenient().when(guardRepository.findForShareBySpecialistId(SPECIALIST_ID)).thenReturn(Optional.of(guard));
	}

	private ScheduleManagementService createService(Clock clock) {
		return new ScheduleManagementService(settingsRepository, weeklyRepository, dateRepository, clock,
				specialistService, guardRepository);
	}

	private void stubSettings() {
		when(settingsRepository.findForUpdateBySpecialistId(SPECIALIST_ID)).thenReturn(Optional.of(settings));
	}

	private void stubEligibleSpecialist() {
		when(specialistService.getEligibleSpecialist(SPECIALIST_ID)).thenReturn(specialist);
	}

	private static CreateWeeklyScheduleVersionCommand weeklyCommand(LocalDate date) {
		return new CreateWeeklyScheduleVersionCommand(date,
				List.of(new WeeklyWorkingIntervalCommand(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0))));
	}

	private static List<WorkingInterval> dateIntervals() {
		return List.of(new WorkingInterval(LocalTime.of(10, 0), LocalTime.of(12, 0)));
	}

	private void setGuardZone(ZoneId zone) {
		guard.changeTimeZone(zone, NOW);
	}

	// ---------------------------------------------------------
	// CHANGE SCHEDULE MODE
	// ---------------------------------------------------------

	@Test
	void changeScheduleMode_updatesModeAndTimestamp() {
		stubSettings();

		service.changeScheduleMode(SPECIALIST_ID, ScheduleMode.FLOATING);

		assertEquals(ScheduleMode.FLOATING, settings.getMode());
		assertEquals(NOW, settings.getUpdatedAt());

		var order = inOrder(guardRepository, settingsRepository);

		order.verify(guardRepository).findForShareBySpecialistId(SPECIALIST_ID);

		order.verify(settingsRepository).findForUpdateBySpecialistId(SPECIALIST_ID);
	}

	@Test
	void changeScheduleMode_sameModeDoesNotChangeTimestamp() {
		stubSettings();

		Instant previousUpdate = settings.getUpdatedAt();

		service.changeScheduleMode(SPECIALIST_ID, ScheduleMode.REGULAR);

		assertEquals(ScheduleMode.REGULAR, settings.getMode());
		assertEquals(previousUpdate, settings.getUpdatedAt());
	}

	@Test
	void changeScheduleMode_rejectsNullArguments() {
		assertThrows(IllegalArgumentException.class, () -> service.changeScheduleMode(null, ScheduleMode.FLOATING));

		assertThrows(IllegalArgumentException.class, () -> service.changeScheduleMode(SPECIALIST_ID, null));

		verifyNoInteractions(guardRepository, settingsRepository);
	}

	@Test
	void changeScheduleMode_failsWhenSettingsMissing() {
		when(settingsRepository.findForUpdateBySpecialistId(SPECIALIST_ID)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class,
				() -> service.changeScheduleMode(SPECIALIST_ID, ScheduleMode.FLOATING));
	}

	// ---------------------------------------------------------
	// CHANGE BOOKING HORIZON
	// ---------------------------------------------------------

	@Test
	void changeBookingHorizon_updatesHorizonAndTimestamp() {
		stubSettings();

		service.changeBookingHorizon(SPECIALIST_ID, 60);

		assertEquals(60, settings.getBookingHorizonDays());
		assertEquals(NOW, settings.getUpdatedAt());

		var order = inOrder(guardRepository, settingsRepository);

		order.verify(guardRepository).findForShareBySpecialistId(SPECIALIST_ID);

		order.verify(settingsRepository).findForUpdateBySpecialistId(SPECIALIST_ID);
	}

	@Test
	void changeBookingHorizon_sameValueDoesNotChangeTimestamp() {
		stubSettings();

		Instant previousUpdate = settings.getUpdatedAt();

		service.changeBookingHorizon(SPECIALIST_ID, 30);

		assertEquals(30, settings.getBookingHorizonDays());
		assertEquals(previousUpdate, settings.getUpdatedAt());
	}

	@Test
	void changeBookingHorizon_rejectsInvalidValues() {
		stubSettings();

		assertThrows(IllegalArgumentException.class, () -> service.changeBookingHorizon(SPECIALIST_ID, 15));

		assertEquals(30, settings.getBookingHorizonDays());
	}

	@Test
	void changeBookingHorizon_rejectsNullSpecialistId() {
		assertThrows(IllegalArgumentException.class, () -> service.changeBookingHorizon(null, 60));

		verifyNoInteractions(guardRepository, settingsRepository);
	}

	@Test
	void changeBookingHorizon_failsWhenSettingsMissing() {
		when(settingsRepository.findForUpdateBySpecialistId(SPECIALIST_ID)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class, () -> service.changeBookingHorizon(SPECIALIST_ID, 60));
	}

	// ---------------------------------------------------------
	// CREATE WEEKLY SCHEDULE VERSION
	// ---------------------------------------------------------

	@Test
	void createWeeklyScheduleVersion_createsVersionWithIntervals() {
		stubSettings();
		stubEligibleSpecialist();

		LocalDate startDate = TODAY.plusDays(7);

		when(weeklyRepository.save(any(WeeklyScheduleVersion.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		WeeklyScheduleVersion result = service.createWeeklyScheduleVersion(SPECIALIST_ID, weeklyCommand(startDate));

		assertEquals(startDate, result.getEffectiveFrom());
		assertNull(result.getEffectiveUntil());
		assertEquals(NOW, result.getCreatedAt());
		assertEquals(1, result.getIntervals().size());

		assertEquals(DayOfWeek.MONDAY, result.getIntervals().get(0).getDayOfWeek());

		verify(weeklyRepository).existsVersionStartingOnOrAfter(SPECIALIST_ID, startDate);

		verify(weeklyRepository).findEffectiveVersion(SPECIALIST_ID, startDate.minusDays(1));

		verify(weeklyRepository).save(result);
	}

	@Test
	void createWeeklyScheduleVersion_createsVersionWithEffectiveUntil() {
		stubSettings();
		stubEligibleSpecialist();

		LocalDate startDate = TODAY.plusDays(7);
		LocalDate endDate = TODAY.plusDays(21);

		CreateWeeklyScheduleVersionCommand command = new CreateWeeklyScheduleVersionCommand(startDate, endDate,
				List.of());

		when(weeklyRepository.save(any(WeeklyScheduleVersion.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		WeeklyScheduleVersion result = service.createWeeklyScheduleVersion(SPECIALIST_ID, command);

		assertEquals(startDate, result.getEffectiveFrom());
		assertEquals(endDate, result.getEffectiveUntil());

		verify(weeklyRepository).save(result);
	}

	@Test
	void createWeeklyScheduleVersion_shortensPreviousVersion() {
		stubSettings();
		stubEligibleSpecialist();

		LocalDate newStart = TODAY.plusDays(14);

		WeeklyScheduleVersion previous = new WeeklyScheduleVersion(specialist, TODAY.minusDays(10),
				NOW.minusSeconds(3600));

		when(weeklyRepository.findEffectiveVersion(SPECIALIST_ID, newStart.minusDays(1)))
				.thenReturn(Optional.of(previous));

		when(weeklyRepository.save(any(WeeklyScheduleVersion.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.createWeeklyScheduleVersion(SPECIALIST_ID, weeklyCommand(newStart));

		assertEquals(newStart.minusDays(1), previous.getEffectiveUntil());
	}

	@Test
	void createWeeklyScheduleVersion_rejectsPastDate() {
		assertThrows(IllegalArgumentException.class,
				() -> service.createWeeklyScheduleVersion(SPECIALIST_ID, weeklyCommand(TODAY.minusDays(1))));

		verify(guardRepository).findForShareBySpecialistId(SPECIALIST_ID);

		verifyNoInteractions(settingsRepository, weeklyRepository);
	}

	@Test
	void createWeeklyScheduleVersion_allowsToday() {
		stubSettings();
		stubEligibleSpecialist();

		when(weeklyRepository.save(any(WeeklyScheduleVersion.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		WeeklyScheduleVersion result = service.createWeeklyScheduleVersion(SPECIALIST_ID, weeklyCommand(TODAY));

		assertEquals(TODAY, result.getEffectiveFrom());
	}

	@Test
	void createWeeklyScheduleVersion_rejectsFloatingMode() {
		settings.changeMode(ScheduleMode.FLOATING, NOW);
		stubSettings();

		assertThrows(IllegalStateException.class,
				() -> service.createWeeklyScheduleVersion(SPECIALIST_ID, weeklyCommand(TODAY.plusDays(1))));

		verifyNoInteractions(weeklyRepository);
	}

	@Test
	void createWeeklyScheduleVersion_rejectsExistingOrFutureVersion() {
		stubSettings();

		LocalDate start = TODAY.plusDays(3);

		when(weeklyRepository.existsVersionStartingOnOrAfter(SPECIALIST_ID, start)).thenReturn(true);

		assertThrows(IllegalStateException.class,
				() -> service.createWeeklyScheduleVersion(SPECIALIST_ID, weeklyCommand(start)));

		verify(weeklyRepository, never()).save(any());
		verifyNoInteractions(specialistService);
	}

	@Test
	void createWeeklyScheduleVersion_rejectsNullArguments() {
		assertThrows(IllegalArgumentException.class,
				() -> service.createWeeklyScheduleVersion(null, weeklyCommand(TODAY)));

		assertThrows(IllegalArgumentException.class, () -> service.createWeeklyScheduleVersion(SPECIALIST_ID, null));

		verifyNoInteractions(guardRepository, settingsRepository);
	}

	@Test
	void createWeeklyScheduleVersion_failsWhenSettingsMissing() {
		when(settingsRepository.findForUpdateBySpecialistId(SPECIALIST_ID)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class,
				() -> service.createWeeklyScheduleVersion(SPECIALIST_ID, weeklyCommand(TODAY.plusDays(1))));

		verifyNoInteractions(weeklyRepository);
	}

	// ---------------------------------------------------------
	// REPLACE DATE SCHEDULE
	// ---------------------------------------------------------

	@Test
	void replaceDateSchedule_createsOverrideInRegularMode() {
		stubSettings();
		stubEligibleSpecialist();

		LocalDate date = TODAY.plusDays(1);

		when(dateRepository.save(any(DateSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

		DateSchedule result = service.replaceDateSchedule(SPECIALIST_ID, date, DateSchedulePurpose.OVERRIDE,
				dateIntervals());

		assertEquals(date, result.getScheduleDate());
		assertEquals(DateSchedulePurpose.OVERRIDE, result.getPurpose());
		assertEquals(1, result.getIntervals().size());

		assertEquals(LocalTime.of(10, 0), result.getIntervals().get(0).getStartTime());

		assertEquals(NOW, result.getCreatedAt());

		verify(dateRepository).save(result);
	}

	@Test
	void replaceDateSchedule_updatesExistingDateSchedule() {
		stubSettings();

		LocalDate date = TODAY.plusDays(2);

		DateSchedule existing = new DateSchedule(specialist, date, DateSchedulePurpose.OVERRIDE,
				NOW.minusSeconds(3600));

		when(dateRepository.findBySpecialistAndDateAndPurpose(SPECIALIST_ID, date, DateSchedulePurpose.OVERRIDE))
				.thenReturn(Optional.of(existing));

		when(dateRepository.save(existing)).thenReturn(existing);

		DateSchedule result = service.replaceDateSchedule(SPECIALIST_ID, date, DateSchedulePurpose.OVERRIDE,
				dateIntervals());

		assertSame(existing, result);
		assertEquals(1, existing.getIntervals().size());
		assertEquals(NOW, existing.getUpdatedAt());

		verifyNoInteractions(specialistService);
	}

	@Test
	void replaceDateSchedule_emptyIntervalsRepresentDayOff() {
		stubSettings();
		stubEligibleSpecialist();

		when(dateRepository.save(any(DateSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

		DateSchedule result = service.replaceDateSchedule(SPECIALIST_ID, TODAY, DateSchedulePurpose.OVERRIDE,
				List.of());

		assertTrue(result.isClosed());
		assertTrue(result.getIntervals().isEmpty());
	}

	@Test
	void replaceDateSchedule_allowsManualInFloatingMode() {
		settings.changeMode(ScheduleMode.FLOATING, NOW);
		stubSettings();
		stubEligibleSpecialist();

		when(dateRepository.save(any(DateSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

		DateSchedule result = service.replaceDateSchedule(SPECIALIST_ID, TODAY.plusDays(1), DateSchedulePurpose.MANUAL,
				dateIntervals());

		assertEquals(DateSchedulePurpose.MANUAL, result.getPurpose());
	}

	@Test
	void replaceDateSchedule_rejectsIncompatibleModeAndPurpose() {
		stubSettings();

		assertThrows(IllegalArgumentException.class,
				() -> service.replaceDateSchedule(SPECIALIST_ID, TODAY, DateSchedulePurpose.MANUAL, dateIntervals()));

		settings.changeMode(ScheduleMode.FLOATING, NOW);

		assertThrows(IllegalArgumentException.class,
				() -> service.replaceDateSchedule(SPECIALIST_ID, TODAY, DateSchedulePurpose.OVERRIDE, dateIntervals()));

		verifyNoInteractions(dateRepository);
	}

	@Test
	void replaceDateSchedule_rejectsPastAndNullArguments() {
		assertThrows(IllegalArgumentException.class,
				() -> service.replaceDateSchedule(null, TODAY, DateSchedulePurpose.OVERRIDE, List.of()));

		assertThrows(IllegalArgumentException.class,
				() -> service.replaceDateSchedule(SPECIALIST_ID, null, DateSchedulePurpose.OVERRIDE, List.of()));

		assertThrows(IllegalArgumentException.class, () -> service.replaceDateSchedule(SPECIALIST_ID,
				TODAY.minusDays(1), DateSchedulePurpose.OVERRIDE, List.of()));

		assertThrows(IllegalArgumentException.class,
				() -> service.replaceDateSchedule(SPECIALIST_ID, TODAY, null, List.of()));

		assertThrows(IllegalArgumentException.class,
				() -> service.replaceDateSchedule(SPECIALIST_ID, TODAY, DateSchedulePurpose.OVERRIDE, null));

		verifyNoInteractions(settingsRepository);
	}

	@Test
	void replaceDateSchedule_rejectsOverlappingIntervals() {
		stubSettings();
		stubEligibleSpecialist();

		List<WorkingInterval> overlapping = List.of(new WorkingInterval(LocalTime.of(9, 0), LocalTime.of(12, 0)),
				new WorkingInterval(LocalTime.of(11, 0), LocalTime.of(13, 0)));

		assertThrows(IllegalArgumentException.class,
				() -> service.replaceDateSchedule(SPECIALIST_ID, TODAY, DateSchedulePurpose.OVERRIDE, overlapping));

		verify(dateRepository, never()).save(any());
	}

	@Test
	void replaceDateSchedule_failsWhenSettingsMissing() {
		when(settingsRepository.findForUpdateBySpecialistId(SPECIALIST_ID)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class,
				() -> service.replaceDateSchedule(SPECIALIST_ID, TODAY, DateSchedulePurpose.OVERRIDE, dateIntervals()));

		verifyNoInteractions(dateRepository);
	}

	// ---------------------------------------------------------
	// REMOVE DATE SCHEDULE
	// ---------------------------------------------------------

	@Test
	void removeDateSchedule_deletesExistingRecord() {
		stubSettings();

		LocalDate date = TODAY.plusDays(1);

		DateSchedule existing = new DateSchedule(specialist, date, DateSchedulePurpose.OVERRIDE, NOW);

		when(dateRepository.findBySpecialistAndDateAndPurpose(SPECIALIST_ID, date, DateSchedulePurpose.OVERRIDE))
				.thenReturn(Optional.of(existing));

		service.removeDateSchedule(SPECIALIST_ID, date, DateSchedulePurpose.OVERRIDE);

		var order = inOrder(guardRepository, settingsRepository, dateRepository);

		order.verify(guardRepository).findForShareBySpecialistId(SPECIALIST_ID);

		order.verify(settingsRepository).findForUpdateBySpecialistId(SPECIALIST_ID);

		order.verify(dateRepository).findBySpecialistAndDateAndPurpose(SPECIALIST_ID, date,
				DateSchedulePurpose.OVERRIDE);

		order.verify(dateRepository).delete(existing);
	}

	@Test
	void removeDateSchedule_failsWhenDateScheduleMissing() {
		stubSettings();

		assertThrows(IllegalStateException.class,
				() -> service.removeDateSchedule(SPECIALIST_ID, TODAY, DateSchedulePurpose.OVERRIDE));

		verify(dateRepository, never()).delete(any());
	}

	@Test
	void removeDateSchedule_failsWhenSettingsMissing() {
		when(settingsRepository.findForUpdateBySpecialistId(SPECIALIST_ID)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class,
				() -> service.removeDateSchedule(SPECIALIST_ID, TODAY, DateSchedulePurpose.OVERRIDE));

		verifyNoInteractions(dateRepository);
	}

	@Test
	void removeDateSchedule_rejectsPastAndNullArguments() {
		assertThrows(IllegalArgumentException.class,
				() -> service.removeDateSchedule(null, TODAY, DateSchedulePurpose.OVERRIDE));

		assertThrows(IllegalArgumentException.class,
				() -> service.removeDateSchedule(SPECIALIST_ID, null, DateSchedulePurpose.OVERRIDE));

		assertThrows(IllegalArgumentException.class,
				() -> service.removeDateSchedule(SPECIALIST_ID, TODAY.minusDays(1), DateSchedulePurpose.OVERRIDE));

		assertThrows(IllegalArgumentException.class, () -> service.removeDateSchedule(SPECIALIST_ID, TODAY, null));

		verifyNoInteractions(settingsRepository, dateRepository);
	}

	// ---------------------------------------------------------
	// TIME ZONE GUARD
	// ---------------------------------------------------------

	@Test
	void changeScheduleMode_failsWhenTimeZoneGuardMissing() {
		when(guardRepository.findForShareBySpecialistId(SPECIALIST_ID)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class,
				() -> service.changeScheduleMode(SPECIALIST_ID, ScheduleMode.FLOATING));

		verifyNoInteractions(settingsRepository);
	}

	@Test
	void changeBookingHorizon_failsWhenTimeZoneGuardMissing() {
		when(guardRepository.findForShareBySpecialistId(SPECIALIST_ID)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class, () -> service.changeBookingHorizon(SPECIALIST_ID, 60));

		verifyNoInteractions(settingsRepository);
	}

	@Test
	void createWeeklyScheduleVersion_failsWhenTimeZoneGuardMissing() {
		when(guardRepository.findForShareBySpecialistId(SPECIALIST_ID)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class,
				() -> service.createWeeklyScheduleVersion(SPECIALIST_ID, weeklyCommand(TODAY)));

		verifyNoInteractions(settingsRepository, weeklyRepository);
	}

	@Test
	void replaceDateSchedule_failsWhenTimeZoneGuardMissing() {
		when(guardRepository.findForShareBySpecialistId(SPECIALIST_ID)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class,
				() -> service.replaceDateSchedule(SPECIALIST_ID, TODAY, DateSchedulePurpose.OVERRIDE, dateIntervals()));

		verifyNoInteractions(settingsRepository, dateRepository);
	}

	@Test
	void removeDateSchedule_failsWhenTimeZoneGuardMissing() {
		when(guardRepository.findForShareBySpecialistId(SPECIALIST_ID)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class,
				() -> service.removeDateSchedule(SPECIALIST_ID, TODAY, DateSchedulePurpose.OVERRIDE));

		verifyNoInteractions(settingsRepository, dateRepository);
	}

	// ---------------------------------------------------------
	// LOCAL DATE IN SPECIALIST TIME ZONE
	// ---------------------------------------------------------

	@Test
	void createWeeklyScheduleVersion_allowsLocalTodayWhenUtcIsTomorrow() {
		Clock boundaryClock = Clock.fixed(Instant.parse("2026-11-06T00:30:00Z"), ZoneOffset.UTC);

		setGuardZone(ZoneId.of("America/Los_Angeles"));
		service = createService(boundaryClock);

		stubSettings();
		stubEligibleSpecialist();

		when(weeklyRepository.save(any(WeeklyScheduleVersion.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		WeeklyScheduleVersion result = service.createWeeklyScheduleVersion(SPECIALIST_ID, weeklyCommand(TODAY));

		assertEquals(TODAY, result.getEffectiveFrom());

		verify(guardRepository).findForShareBySpecialistId(SPECIALIST_ID);
	}

	@Test
	void createWeeklyScheduleVersion_rejectsLocalYesterdayWhenUtcIsToday() {
		Clock boundaryClock = Clock.fixed(Instant.parse("2026-11-05T23:30:00Z"), ZoneOffset.UTC);

		setGuardZone(ZoneId.of("Asia/Tokyo"));
		service = createService(boundaryClock);

		assertThrows(IllegalArgumentException.class,
				() -> service.createWeeklyScheduleVersion(SPECIALIST_ID, weeklyCommand(TODAY)));

		verifyNoInteractions(settingsRepository, weeklyRepository);
	}

	@Test
	void replaceDateSchedule_allowsLocalTodayWhenUtcIsTomorrow() {
		Clock boundaryClock = Clock.fixed(Instant.parse("2026-11-06T00:30:00Z"), ZoneOffset.UTC);

		setGuardZone(ZoneId.of("America/Los_Angeles"));
		service = createService(boundaryClock);

		stubSettings();
		stubEligibleSpecialist();

		when(dateRepository.save(any(DateSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

		DateSchedule result = service.replaceDateSchedule(SPECIALIST_ID, TODAY, DateSchedulePurpose.OVERRIDE,
				dateIntervals());

		assertEquals(TODAY, result.getScheduleDate());
		assertEquals(1, result.getIntervals().size());
	}

	@Test
	void replaceDateSchedule_rejectsLocalYesterdayWhenUtcIsToday() {
		Clock boundaryClock = Clock.fixed(Instant.parse("2026-11-05T23:30:00Z"), ZoneOffset.UTC);

		setGuardZone(ZoneId.of("Asia/Tokyo"));
		service = createService(boundaryClock);

		assertThrows(IllegalArgumentException.class,
				() -> service.replaceDateSchedule(SPECIALIST_ID, TODAY, DateSchedulePurpose.OVERRIDE, dateIntervals()));

		verifyNoInteractions(settingsRepository, dateRepository);
	}

	@Test
	void removeDateSchedule_allowsLocalTodayWhenUtcIsTomorrow() {
		Clock boundaryClock = Clock.fixed(Instant.parse("2026-11-06T00:30:00Z"), ZoneOffset.UTC);

		setGuardZone(ZoneId.of("America/Los_Angeles"));
		service = createService(boundaryClock);

		stubSettings();

		DateSchedule existing = new DateSchedule(specialist, TODAY, DateSchedulePurpose.OVERRIDE, NOW);

		when(dateRepository.findBySpecialistAndDateAndPurpose(SPECIALIST_ID, TODAY, DateSchedulePurpose.OVERRIDE))
				.thenReturn(Optional.of(existing));

		service.removeDateSchedule(SPECIALIST_ID, TODAY, DateSchedulePurpose.OVERRIDE);

		verify(dateRepository).delete(existing);
	}

	@Test
	void removeDateSchedule_rejectsLocalYesterdayWhenUtcIsToday() {
		Clock boundaryClock = Clock.fixed(Instant.parse("2026-11-05T23:30:00Z"), ZoneOffset.UTC);

		setGuardZone(ZoneId.of("Asia/Tokyo"));
		service = createService(boundaryClock);

		assertThrows(IllegalArgumentException.class,
				() -> service.removeDateSchedule(SPECIALIST_ID, TODAY, DateSchedulePurpose.OVERRIDE));

		verifyNoInteractions(settingsRepository, dateRepository);
	}

	// ---------------------------------------------------------
	// LOCK ACQUISITION ORDER
	// ---------------------------------------------------------

	@Test
	void createWeeklyScheduleVersion_acquiresLocksInCorrectOrder() {
		stubSettings();
		stubEligibleSpecialist();

		when(weeklyRepository.save(any(WeeklyScheduleVersion.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.createWeeklyScheduleVersion(SPECIALIST_ID, weeklyCommand(TODAY.plusDays(1)));

		var order = inOrder(guardRepository, settingsRepository);

		order.verify(guardRepository).findForShareBySpecialistId(SPECIALIST_ID);

		order.verify(settingsRepository).findForUpdateBySpecialistId(SPECIALIST_ID);
	}

	@Test
	void replaceDateSchedule_acquiresLocksInCorrectOrder() {
		stubSettings();
		stubEligibleSpecialist();

		when(dateRepository.save(any(DateSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.replaceDateSchedule(SPECIALIST_ID, TODAY, DateSchedulePurpose.OVERRIDE, dateIntervals());

		var order = inOrder(guardRepository, settingsRepository);

		order.verify(guardRepository).findForShareBySpecialistId(SPECIALIST_ID);

		order.verify(settingsRepository).findForUpdateBySpecialistId(SPECIALIST_ID);
	}

	@Test
	void createWeeklyScheduleVersionCommand_acceptsFinitePeriod() {
		LocalDate from = LocalDate.of(2026, 12, 1);
		LocalDate until = LocalDate.of(2026, 12, 20);

		CreateWeeklyScheduleVersionCommand command = new CreateWeeklyScheduleVersionCommand(from, until, List.of());

		assertEquals(from, command.effectiveFrom());
		assertEquals(until, command.effectiveUntil());
	}

	@Test
	void createWeeklyScheduleVersionCommand_acceptsOpenEndedPeriod() {
		LocalDate from = LocalDate.of(2026, 12, 1);

		CreateWeeklyScheduleVersionCommand command = new CreateWeeklyScheduleVersionCommand(from, List.of());

		assertEquals(from, command.effectiveFrom());
		assertNull(command.effectiveUntil());
	}

	@Test
	void createWeeklyScheduleVersionCommand_rejectsInvalidPeriod() {
		assertThrows(IllegalArgumentException.class,
				() -> new CreateWeeklyScheduleVersionCommand(LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 1),
						List.of()));
	}
}