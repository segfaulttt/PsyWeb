package com.psyweb.schedule.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.psyweb.schedule.domain.ScheduleMode;
import com.psyweb.schedule.domain.SpecialistScheduleSettings;
import com.psyweb.schedule.domain.SpecialistTimeZoneGuard;
import com.psyweb.schedule.repository.SpecialistScheduleSettingsRepository;
import com.psyweb.schedule.repository.SpecialistTimeZoneGuardRepository;
import com.psyweb.specialist.domain.Specialist;

@ExtendWith(MockitoExtension.class)
class ScheduleInitializationServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

	private static final ZoneId TIME_ZONE = ZoneId.of("Europe/Amsterdam");

	@Mock
	private SpecialistScheduleSettingsRepository settingsRepository;

	@Mock
	private SpecialistTimeZoneGuardRepository guardRepository;

	@Mock
	private Specialist specialist;

	private ScheduleInitializationService service;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

		service = new ScheduleInitializationService(settingsRepository, guardRepository, clock);
	}

	@Test
	void shouldInitializeScheduleSettingsAndTimeZoneGuard() {
		service.initialize(specialist, TIME_ZONE);

		ArgumentCaptor<SpecialistScheduleSettings> settingsCaptor = ArgumentCaptor
				.forClass(SpecialistScheduleSettings.class);

		ArgumentCaptor<SpecialistTimeZoneGuard> guardCaptor = ArgumentCaptor.forClass(SpecialistTimeZoneGuard.class);

		verify(settingsRepository).save(settingsCaptor.capture());
		verify(guardRepository).save(guardCaptor.capture());

		assertNotNull(settingsCaptor.getValue());
		assertNotNull(guardCaptor.getValue());

		verifyNoMoreInteractions(settingsRepository, guardRepository);
	}

	@Test
	void shouldInitializeDefaultRegularScheduleWithThirtyDayHorizon() {
		service.initialize(specialist, TIME_ZONE);

		ArgumentCaptor<SpecialistScheduleSettings> captor = ArgumentCaptor.forClass(SpecialistScheduleSettings.class);

		verify(settingsRepository).save(captor.capture());

		SpecialistScheduleSettings settings = captor.getValue();

		assertEquals(ScheduleMode.REGULAR, settings.getMode());
		assertEquals(30, settings.getBookingHorizonDays());
		assertEquals(NOW, settings.getUpdatedAt());
	}

	@Test
	void shouldInitializeGuardWithProvidedTimeZone() {
		service.initialize(specialist, TIME_ZONE);

		ArgumentCaptor<SpecialistTimeZoneGuard> captor = ArgumentCaptor.forClass(SpecialistTimeZoneGuard.class);

		verify(guardRepository).save(captor.capture());

		SpecialistTimeZoneGuard guard = captor.getValue();

		assertEquals(TIME_ZONE, guard.getTimeZone());
		assertEquals(NOW, guard.getUpdatedAt());
	}

	@Test
	void shouldUseSameTimestampForSettingsAndGuard() {
		service.initialize(specialist, TIME_ZONE);

		ArgumentCaptor<SpecialistScheduleSettings> settingsCaptor = ArgumentCaptor
				.forClass(SpecialistScheduleSettings.class);

		ArgumentCaptor<SpecialistTimeZoneGuard> guardCaptor = ArgumentCaptor.forClass(SpecialistTimeZoneGuard.class);

		verify(settingsRepository).save(settingsCaptor.capture());
		verify(guardRepository).save(guardCaptor.capture());

		Instant settingsTime = settingsCaptor.getValue().getUpdatedAt();

		Instant guardTime = guardCaptor.getValue().getUpdatedAt();

		assertEquals(NOW, settingsTime);
		assertEquals(NOW, guardTime);
		assertEquals(settingsTime, guardTime);
	}

	@Test
	void shouldSaveSettingsBeforeTimeZoneGuard() {
		service.initialize(specialist, TIME_ZONE);

		var order = inOrder(settingsRepository, guardRepository);

		order.verify(settingsRepository).save(any(SpecialistScheduleSettings.class));

		order.verify(guardRepository).save(any(SpecialistTimeZoneGuard.class));
	}

	@Test
	void shouldSupportDifferentIanaTimeZones() {
		ZoneId tokyo = ZoneId.of("Asia/Tokyo");

		service.initialize(specialist, tokyo);

		ArgumentCaptor<SpecialistTimeZoneGuard> captor = ArgumentCaptor.forClass(SpecialistTimeZoneGuard.class);

		verify(guardRepository).save(captor.capture());

		assertEquals(tokyo, captor.getValue().getTimeZone());
	}

	@Test
	void shouldRejectNullSpecialistWithoutPersistence() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> service.initialize(null, TIME_ZONE));

		assertEquals("Specialist and time zone are required", exception.getMessage());

		verifyNoInteractions(settingsRepository, guardRepository);
	}

	@Test
	void shouldRejectNullTimeZoneWithoutPersistence() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> service.initialize(specialist, null));

		assertEquals("Specialist and time zone are required", exception.getMessage());

		verifyNoInteractions(settingsRepository, guardRepository);
	}

	@Test
	void shouldRejectBothNullArgumentsWithoutPersistence() {
		assertThrows(IllegalArgumentException.class, () -> service.initialize(null, null));

		verifyNoInteractions(settingsRepository, guardRepository);
	}

	@Test
	void shouldNotSaveGuardWhenSettingsPersistenceFails() {
		RuntimeException failure = new RuntimeException("Settings persistence failed");

		when(settingsRepository.save(any(SpecialistScheduleSettings.class))).thenThrow(failure);

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> service.initialize(specialist, TIME_ZONE));

		assertSame(failure, exception);

		verify(settingsRepository).save(any(SpecialistScheduleSettings.class));

		verifyNoInteractions(guardRepository);
	}

	@Test
	void shouldPropagateGuardPersistenceFailure() {
		RuntimeException failure = new RuntimeException("Time zone guard persistence failed");

		when(guardRepository.save(any(SpecialistTimeZoneGuard.class))).thenThrow(failure);

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> service.initialize(specialist, TIME_ZONE));

		assertSame(failure, exception);

		verify(settingsRepository).save(any(SpecialistScheduleSettings.class));

		verify(guardRepository).save(any(SpecialistTimeZoneGuard.class));
	}
}