package com.psyweb.booking.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.psyweb.booking.domain.Booking;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.repository.BookingRepository;
import com.psyweb.booking.repository.ReservationRepository;
import com.psyweb.booking.service.model.BookingObligation;
import com.psyweb.booking.service.model.ObligationType;

@ExtendWith(MockitoExtension.class)
class BookingObligationsQueryServiceTest {

	private static final Long SPECIALIST_ID = 1L;
	private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

	private static final Instant START = Instant.parse("2026-11-06T12:00:00Z");

	private static final Instant END = Instant.parse("2026-11-06T13:00:00Z");

	@Mock
	private BookingRepository bookingRepository;

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private Booking booking;

	@Mock
	private Reservation reservation;

	private BookingObligationsQueryService service;

	@BeforeEach
	void setUp() {
		service = new BookingObligationsQueryService(bookingRepository, reservationRepository);
	}

	@Test
	void getFutureConfirmedBookings_mapsBookingsToObligations() {
		when(bookingRepository.findFutureConfirmedBookingsBySpecialistId(SPECIALIST_ID, NOW))
				.thenReturn(List.of(booking));

		when(booking.getId()).thenReturn(10L);
		when(booking.getStartTime()).thenReturn(START);
		when(booking.getEndTime()).thenReturn(END);

		List<BookingObligation> result = service.getFutureConfirmedBookings(SPECIALIST_ID, NOW);

		assertEquals(1, result.size());

		BookingObligation obligation = result.get(0);

		assertEquals(10L, obligation.id());
		assertEquals(START, obligation.startTime());
		assertEquals(END, obligation.endTime());
		assertEquals(ObligationType.BOOKING, obligation.type());

		verify(bookingRepository).findFutureConfirmedBookingsBySpecialistId(SPECIALIST_ID, NOW);

		verifyNoInteractions(reservationRepository);
	}

	@Test
	void getFutureConfirmedBookings_returnsEmptyList() {
		when(bookingRepository.findFutureConfirmedBookingsBySpecialistId(SPECIALIST_ID, NOW)).thenReturn(List.of());

		List<BookingObligation> result = service.getFutureConfirmedBookings(SPECIALIST_ID, NOW);

		assertTrue(result.isEmpty());
	}

	@Test
	void getFutureConfirmedBookings_mapsMultipleBookings() {
		Booking secondBooking = mock(Booking.class);

		when(bookingRepository.findFutureConfirmedBookingsBySpecialistId(SPECIALIST_ID, NOW))
				.thenReturn(List.of(booking, secondBooking));

		when(booking.getId()).thenReturn(10L);
		when(booking.getStartTime()).thenReturn(START);
		when(booking.getEndTime()).thenReturn(END);

		Instant secondStart = START.plusSeconds(7200);
		Instant secondEnd = END.plusSeconds(7200);

		when(secondBooking.getId()).thenReturn(11L);
		when(secondBooking.getStartTime()).thenReturn(secondStart);
		when(secondBooking.getEndTime()).thenReturn(secondEnd);

		List<BookingObligation> result = service.getFutureConfirmedBookings(SPECIALIST_ID, NOW);

		assertEquals(2, result.size());
		assertEquals(10L, result.get(0).id());
		assertEquals(11L, result.get(1).id());
		assertEquals(secondStart, result.get(1).startTime());
	}

	@Test
	void getFutureConfirmedBookings_rejectsNullSpecialistId() {
		assertThrows(IllegalArgumentException.class, () -> service.getFutureConfirmedBookings(null, NOW));

		verifyNoInteractions(bookingRepository, reservationRepository);
	}

	@Test
	void getFutureConfirmedBookings_rejectsNullNow() {
		assertThrows(IllegalArgumentException.class, () -> service.getFutureConfirmedBookings(SPECIALIST_ID, null));

		verifyNoInteractions(bookingRepository, reservationRepository);
	}

	@Test
	void getActiveFutureReservations_mapsReservationsToObligations() {
		when(reservationRepository.findActiveFutureReservationsBySpecialistId(SPECIALIST_ID, NOW))
				.thenReturn(List.of(reservation));

		when(reservation.getId()).thenReturn(20L);
		when(reservation.getStartTime()).thenReturn(START);
		when(reservation.getEndTime()).thenReturn(END);

		List<BookingObligation> result = service.getActiveFutureReservations(SPECIALIST_ID, NOW);

		assertEquals(1, result.size());

		BookingObligation obligation = result.get(0);

		assertEquals(20L, obligation.id());
		assertEquals(START, obligation.startTime());
		assertEquals(END, obligation.endTime());
		assertEquals(ObligationType.RESERVATION, obligation.type());

		verifyNoInteractions(bookingRepository);
	}

	@Test
	void getActiveFutureReservations_returnsEmptyList() {
		when(reservationRepository.findActiveFutureReservationsBySpecialistId(SPECIALIST_ID, NOW))
				.thenReturn(List.of());

		List<BookingObligation> result = service.getActiveFutureReservations(SPECIALIST_ID, NOW);

		assertTrue(result.isEmpty());
	}

	@Test
	void getActiveFutureReservations_mapsMultipleReservations() {
		Reservation secondReservation = mock(Reservation.class);

		when(reservationRepository.findActiveFutureReservationsBySpecialistId(SPECIALIST_ID, NOW))
				.thenReturn(List.of(reservation, secondReservation));

		when(reservation.getId()).thenReturn(20L);
		when(reservation.getStartTime()).thenReturn(START);
		when(reservation.getEndTime()).thenReturn(END);

		when(secondReservation.getId()).thenReturn(21L);
		when(secondReservation.getStartTime()).thenReturn(START.plusSeconds(7200));
		when(secondReservation.getEndTime()).thenReturn(END.plusSeconds(7200));

		List<BookingObligation> result = service.getActiveFutureReservations(SPECIALIST_ID, NOW);

		assertEquals(2, result.size());
		assertEquals(20L, result.get(0).id());
		assertEquals(21L, result.get(1).id());
		assertEquals(ObligationType.RESERVATION, result.get(1).type());
	}

	@Test
	void getActiveFutureReservations_rejectsNullSpecialistId() {
		assertThrows(IllegalArgumentException.class, () -> service.getActiveFutureReservations(null, NOW));

		verifyNoInteractions(bookingRepository, reservationRepository);
	}

	@Test
	void getActiveFutureReservations_rejectsNullNow() {
		assertThrows(IllegalArgumentException.class, () -> service.getActiveFutureReservations(SPECIALIST_ID, null));

		verifyNoInteractions(bookingRepository, reservationRepository);
	}
}