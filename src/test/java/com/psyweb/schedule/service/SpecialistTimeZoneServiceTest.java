package com.psyweb.schedule.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.psyweb.booking.service.BookingObligationsQueryService;
import com.psyweb.booking.service.model.BookingObligation;
import com.psyweb.booking.service.model.ObligationType;
import com.psyweb.schedule.domain.SpecialistTimeZoneGuard;
import com.psyweb.schedule.repository.SpecialistTimeZoneGuardRepository;
import com.psyweb.schedule.service.model.TimeZoneChangePreview;
import com.psyweb.specialist.domain.Specialist;

@ExtendWith(MockitoExtension.class)
class SpecialistTimeZoneServiceTest {

	private static final Long SPECIALIST_ID = 1L;

	private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

	private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

	private static final ZoneId AMSTERDAM = ZoneId.of("Europe/Amsterdam");

	private static final Instant START = Instant.parse("2026-11-06T12:00:00Z");

	private static final Instant END = Instant.parse("2026-11-06T13:00:00Z");

	@Mock
	private SpecialistTimeZoneGuardRepository guardRepository;

	@Mock
	private BookingObligationsQueryService obligationsQueryService;

	@Mock
	private Specialist specialist;

	private SpecialistTimeZoneService service;
	private SpecialistTimeZoneGuard guard;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

		service = new SpecialistTimeZoneService(guardRepository, obligationsQueryService, clock);

		guard = new SpecialistTimeZoneGuard(specialist, MOSCOW, NOW.minusSeconds(3600));
	}

	private void stubGuardForRead() {
		when(guardRepository.findById(SPECIALIST_ID)).thenReturn(Optional.of(guard));
	}

	private void stubGuardForUpdate() {
		when(guardRepository.findForUpdateBySpecialistId(SPECIALIST_ID)).thenReturn(Optional.of(guard));
	}

	private void stubEmptyObligations() {
		when(obligationsQueryService.getFutureConfirmedBookings(SPECIALIST_ID, NOW)).thenReturn(List.of());

		when(obligationsQueryService.getActiveFutureReservations(SPECIALIST_ID, NOW)).thenReturn(List.of());
	}

	private static BookingObligation booking(Instant start, Instant end) {
		return new BookingObligation(10L, start, end, ObligationType.BOOKING);
	}

	private static BookingObligation reservation(Instant start, Instant end) {
		return new BookingObligation(20L, start, end, ObligationType.RESERVATION);
	}

	// ---------------------------------------------------------
	// getTimeZone
	// ---------------------------------------------------------

	@Test
	void getTimeZone_returnsCurrentZone() {
		stubGuardForRead();

		ZoneId result = service.getTimeZone(SPECIALIST_ID);

		assertEquals(MOSCOW, result);

		verify(guardRepository).findById(SPECIALIST_ID);
	}

	@Test
	void getTimeZone_rejectsNullSpecialistId() {
		assertThrows(IllegalArgumentException.class, () -> service.getTimeZone(null));

		verifyNoInteractions(guardRepository);
	}

	@Test
	void getTimeZone_failsWhenGuardMissing() {
		when(guardRepository.findById(SPECIALIST_ID)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class, () -> service.getTimeZone(SPECIALIST_ID));
	}

	// ---------------------------------------------------------
	// previewTimeZoneChange
	// ---------------------------------------------------------

	@Test
	void previewTimeZoneChange_countsAffectedBookingsAndReservations() {
		stubGuardForRead();

		when(obligationsQueryService.getFutureConfirmedBookings(SPECIALIST_ID, NOW))
				.thenReturn(List.of(booking(START, END), booking(START.plusSeconds(3600), END.plusSeconds(3600))));

		when(obligationsQueryService.getActiveFutureReservations(SPECIALIST_ID, NOW))
				.thenReturn(List.of(reservation(START, END)));

		TimeZoneChangePreview result = service.previewTimeZoneChange(SPECIALIST_ID, AMSTERDAM);

		assertEquals(MOSCOW, result.currentZoneId());
		assertEquals(AMSTERDAM, result.newZoneId());

		assertEquals(2, result.affectedBookingsCount());
		assertEquals(1, result.affectedReservationsCount());

		assertTrue(result.requiresAcknowledgement());
	}

	@Test
	void previewTimeZoneChange_noObligationsRequiresNoAcknowledgement() {
		stubGuardForRead();
		stubEmptyObligations();

		TimeZoneChangePreview result = service.previewTimeZoneChange(SPECIALIST_ID, AMSTERDAM);

		assertEquals(0, result.affectedBookingsCount());
		assertEquals(0, result.affectedReservationsCount());

		assertFalse(result.requiresAcknowledgement());
	}

	@Test
	void previewTimeZoneChange_sameZoneDoesNotAffectObligations() {
		stubGuardForRead();

		when(obligationsQueryService.getFutureConfirmedBookings(SPECIALIST_ID, NOW))
				.thenReturn(List.of(booking(START, END)));

		when(obligationsQueryService.getActiveFutureReservations(SPECIALIST_ID, NOW)).thenReturn(List.of());

		TimeZoneChangePreview result = service.previewTimeZoneChange(SPECIALIST_ID, MOSCOW);

		assertEquals(0, result.affectedBookingsCount());
		assertEquals(0, result.affectedReservationsCount());
		assertFalse(result.requiresAcknowledgement());
	}

	@Test
	void previewTimeZoneChange_equalOffsetsDoNotAffectLocalTime() {
		stubGuardForRead();

		ZoneId paris = ZoneId.of("Europe/Paris");

		guard.changeTimeZone(AMSTERDAM, NOW);

		when(obligationsQueryService.getFutureConfirmedBookings(SPECIALIST_ID, NOW))
				.thenReturn(List.of(booking(START, END)));

		when(obligationsQueryService.getActiveFutureReservations(SPECIALIST_ID, NOW)).thenReturn(List.of());

		TimeZoneChangePreview result = service.previewTimeZoneChange(SPECIALIST_ID, paris);

		assertEquals(0, result.affectedBookingsCount());
		assertFalse(result.requiresAcknowledgement());
	}

	@Test
	void previewTimeZoneChange_handlesDifferentDstOffsets() {
		stubGuardForRead();

		Instant summerStart = Instant.parse("2026-07-10T12:00:00Z");

		Instant summerEnd = Instant.parse("2026-07-10T13:00:00Z");

		when(obligationsQueryService.getFutureConfirmedBookings(SPECIALIST_ID, NOW))
				.thenReturn(List.of(booking(summerStart, summerEnd)));

		when(obligationsQueryService.getActiveFutureReservations(SPECIALIST_ID, NOW)).thenReturn(List.of());

		TimeZoneChangePreview result = service.previewTimeZoneChange(SPECIALIST_ID, AMSTERDAM);

		assertEquals(1, result.affectedBookingsCount());
		assertTrue(result.requiresAcknowledgement());
	}

	@Test
	void previewTimeZoneChange_rejectsNullArguments() {
		assertThrows(IllegalArgumentException.class, () -> service.previewTimeZoneChange(null, AMSTERDAM));

		assertThrows(IllegalArgumentException.class, () -> service.previewTimeZoneChange(SPECIALIST_ID, null));

		verifyNoInteractions(guardRepository, obligationsQueryService);
	}

	@Test
	void previewTimeZoneChange_failsWhenGuardMissing() {
		when(guardRepository.findById(SPECIALIST_ID)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class, () -> service.previewTimeZoneChange(SPECIALIST_ID, AMSTERDAM));

		verifyNoInteractions(obligationsQueryService);
	}

	// ---------------------------------------------------------
	// changeTimeZone
	// ---------------------------------------------------------

	@Test
	void changeTimeZone_updatesZoneWhenNoObligations() {
		stubGuardForUpdate();
		stubEmptyObligations();

		service.changeTimeZone(SPECIALIST_ID, AMSTERDAM, false);

		assertEquals(AMSTERDAM, guard.getTimeZone());
		assertEquals(NOW, guard.getUpdatedAt());

		verify(guardRepository).findForUpdateBySpecialistId(SPECIALIST_ID);
	}

	@Test
	void changeTimeZone_updatesZoneWhenAcknowledged() {
		stubGuardForUpdate();

		when(obligationsQueryService.getFutureConfirmedBookings(SPECIALIST_ID, NOW))
				.thenReturn(List.of(booking(START, END)));

		when(obligationsQueryService.getActiveFutureReservations(SPECIALIST_ID, NOW))
				.thenReturn(List.of(reservation(START, END)));

		service.changeTimeZone(SPECIALIST_ID, AMSTERDAM, true);

		assertEquals(AMSTERDAM, guard.getTimeZone());
		assertEquals(NOW, guard.getUpdatedAt());
	}

	@Test
	void changeTimeZone_rejectsMissingAcknowledgement() {
		stubGuardForUpdate();

		when(obligationsQueryService.getFutureConfirmedBookings(SPECIALIST_ID, NOW))
				.thenReturn(List.of(booking(START, END)));

		when(obligationsQueryService.getActiveFutureReservations(SPECIALIST_ID, NOW)).thenReturn(List.of());

		assertThrows(IllegalStateException.class, () -> service.changeTimeZone(SPECIALIST_ID, AMSTERDAM, false));

		assertEquals(MOSCOW, guard.getTimeZone());
		assertEquals(NOW.minusSeconds(3600), guard.getUpdatedAt());
	}

	@Test
	void changeTimeZone_sameZoneDoesNothing() {
		stubGuardForUpdate();

		Instant previousUpdate = guard.getUpdatedAt();

		service.changeTimeZone(SPECIALIST_ID, MOSCOW, false);

		assertEquals(MOSCOW, guard.getTimeZone());
		assertEquals(previousUpdate, guard.getUpdatedAt());

		verifyNoInteractions(obligationsQueryService);
	}

	@Test
	void changeTimeZone_rejectsNullArguments() {
		assertThrows(IllegalArgumentException.class, () -> service.changeTimeZone(null, AMSTERDAM, false));

		assertThrows(IllegalArgumentException.class, () -> service.changeTimeZone(SPECIALIST_ID, null, false));

		verifyNoInteractions(guardRepository, obligationsQueryService);
	}

	@Test
	void changeTimeZone_failsWhenGuardMissing() {
		when(guardRepository.findForUpdateBySpecialistId(SPECIALIST_ID)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class, () -> service.changeTimeZone(SPECIALIST_ID, AMSTERDAM, true));

		verifyNoInteractions(obligationsQueryService);
	}

	@Test
	void changeTimeZone_checksObligationsAfterAcquiringGuard() {
		stubGuardForUpdate();
		stubEmptyObligations();

		service.changeTimeZone(SPECIALIST_ID, AMSTERDAM, false);

		var order = inOrder(guardRepository, obligationsQueryService);

		order.verify(guardRepository).findForUpdateBySpecialistId(SPECIALIST_ID);

		order.verify(obligationsQueryService).getFutureConfirmedBookings(SPECIALIST_ID, NOW);

		order.verify(obligationsQueryService).getActiveFutureReservations(SPECIALIST_ID, NOW);
	}
}