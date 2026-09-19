package com.psyweb.booking.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.service.AvailabilitySlotService;
import com.psyweb.booking.domain.Booking;
import com.psyweb.booking.domain.BookingStatus;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.domain.ReservationStatus;
import com.psyweb.booking.exception.BookingNotFoundException;
import com.psyweb.booking.exception.InvalidBookingDataException;
import com.psyweb.booking.exception.InvalidBookingStateException;
import com.psyweb.booking.exception.InvalidReservationDataException;
import com.psyweb.booking.exception.InvalidReservationStateException;
import com.psyweb.booking.exception.ReservationExpiredException;
import com.psyweb.booking.exception.ReservationOwnershipException;
import com.psyweb.booking.repository.BookingRepository;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.exception.InvalidSpecialistDataException;
import com.psyweb.specialist.service.SpecialistService;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.service.UserService;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

	private BookingService bookingService;
	private Specialist specialist;
	private User client;
	private AvailabilitySlot slot;
	private Reservation reservation;
	private final Clock clock = Clock.fixed(Instant.parse("2099-01-01T10:00:00Z"), ZoneId.of("UTC"));
	private final LocalDateTime now = LocalDateTime.now(clock);

	@Mock
	BookingRepository bookingRepository;

	@Mock
	ReservationService reservationService;

	@Mock
	AvailabilitySlotService slotService;

	@Mock
	UserService userService;

	@Mock
	SpecialistService specialistService;

	@BeforeEach
	void setUp() {
		bookingService = new BookingService(bookingRepository, userService, specialistService, slotService,
				reservationService, clock);

		client = new User("example@email.ru", "password", UserRole.CLIENT, UserStatus.ACTIVE);

		User user = new User("email@gmail.com", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);

		specialist = new Specialist(user, "firstName", "lastName", Duration.ZERO, Duration.ZERO);

		slot = new AvailabilitySlot(specialist, now, now.plusHours(1));

		reservation = new Reservation(client, slot, now, now.plusMinutes(2));

		ReflectionTestUtils.setField(client, "id", 1L);
		ReflectionTestUtils.setField(specialist, "id", 2L);
		ReflectionTestUtils.setField(slot, "id", 10L);
		ReflectionTestUtils.setField(reservation, "id", 100L);
	}

	@Test
	void shouldConfirmActiveReservationAndBookReservedSlot() {
		Long reservationId = 100L;
		Long clientId = 1L;
		slot.reserve();

		assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
		assertEquals(AvailabilityStatus.RESERVED, slot.getAvailabilityStatus());

		when(reservationService.getReservationForUpdate(reservationId)).thenReturn(reservation);
		when(userService.getActiveUser(clientId)).thenReturn(client);
		when(specialistService.getEligibleSpecialist(slot.getSpecialistId())).thenReturn(specialist);
		when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(slotService.confirmBooking(slot.getId())).thenAnswer(invocation -> {
			slot.confirmBooking();
			return slot;
		});

		Booking result = bookingService.confirmReservation(reservationId, clientId);

		assertEquals(specialist.getId(), result.getSpecialistId());
		assertEquals(client.getId(), result.getClientId());
		assertEquals(slot.getId(), result.getSlotId());
		assertEquals(reservation.getId(), result.getReservationId());
		assertEquals(now, result.getCreatedAt());

		assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
		assertEquals(AvailabilityStatus.BOOKED, slot.getAvailabilityStatus());
		assertEquals(BookingStatus.CONFIRMED, result.getStatus());

		verify(bookingRepository).save(any(Booking.class));
		verify(slotService).confirmBooking(slot.getId());
	}

	@Test
	void shouldThrowWhenReservationIdIsNull() {
		Long reservationId = null;
		Long clientId = 1L;

		InvalidReservationDataException exception = assertThrows(InvalidReservationDataException.class,
				() -> bookingService.confirmReservation(reservationId, clientId));

		assertEquals("RESERVATION_INVALID_DATA", exception.code());
		assertEquals("Incorrect reservation id", exception.getMessage());
		verify(bookingRepository, never()).save(any());
	}

	@Test
	void shouldThrowWhenClientIdIsNull() {
		Long reservationId = 100L;
		Long clientId = null;

		InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
				() -> bookingService.confirmReservation(reservationId, clientId));

		assertEquals("USER_INVALID_DATA", exception.code());
		assertEquals("Incorrect client id", exception.getMessage());
		verify(bookingRepository, never()).save(any());
	}

	@Test
	void shouldThrowWhenReservationBelongsToAnotherClient() {
		Long reservationId = 100L;
		Long clientId = 3L;

		when(reservationService.getReservationForUpdate(reservationId)).thenReturn(reservation);

		ReservationOwnershipException exception = assertThrows(ReservationOwnershipException.class,
				() -> bookingService.confirmReservation(reservationId, clientId));

		assertEquals("RESERVATION_OWNERSHIP_VIOLATION", exception.code());
		assertEquals("Reservation does not belong to this client", exception.getMessage());
		assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
		verify(bookingRepository, never()).save(any());
	}

	@Test
	void shouldExpireReservationAndRejectConfirmationWhenExpired() {
		Long reservationId = 100L;
		Long clientId = 1L;
		Reservation expired = mock(Reservation.class);

		when(reservationService.getReservationForUpdate(reservationId)).thenReturn(expired);
		when(expired.getClientId()).thenReturn(clientId);
		when(expired.isExpired(now)).thenReturn(true);

		ReservationExpiredException exception = assertThrows(ReservationExpiredException.class,
				() -> bookingService.confirmReservation(reservationId, clientId));

		assertEquals("RESERVATION_EXPIRED", exception.code());
		assertEquals("Reservation already expired", exception.getMessage());
		verify(reservationService).expireReservation(reservationId);
		verify(bookingRepository, never()).save(any());
		verifyNoInteractions(userService, specialistService);
	}

	@Test
	void shouldThrowWhenReservationIsNotActive() {
		Long reservationId = 100L;
		Long clientId = 1L;
		reservation.cancel();

		when(reservationService.getReservationForUpdate(reservationId)).thenReturn(reservation);

		InvalidReservationStateException exception = assertThrows(InvalidReservationStateException.class,
				() -> bookingService.confirmReservation(reservationId, clientId));

		assertEquals("RESERVATION_INVALID_STATE", exception.code());
		assertEquals("Reservation must have status 'ACTIVE'", exception.getMessage());
		assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
		verify(bookingRepository, never()).save(any());
	}

	@Test
	void shouldCancelBooking() {
		Long bookingId = 1L;
		ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);
		slot.reserve();
		slot.confirmBooking();

		Booking booking = new Booking(client, specialist, slot, reservation, now.minusMinutes(1));
		ReflectionTestUtils.setField(booking, "id", bookingId);

		when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
		when(slotService.releaseBooking(slot.getId())).thenReturn(slot);

		bookingService.cancelBooking(bookingId);

		assertEquals(BookingStatus.CANCELLED, booking.getStatus());
		assertNotEquals(null, booking.getCancelledAt());
		assertEquals(now, booking.getCancelledAt());
		verify(slotService).releaseBooking(slot.getId());
	}

	@Test
	void shouldThrowWhenBookingIdIsNull() {
		Long bookingId = null;

		InvalidBookingDataException exception = assertThrows(InvalidBookingDataException.class,
				() -> bookingService.cancelBooking(bookingId));

		assertEquals("BOOKING_INVALID_DATA", exception.code());
		assertEquals("Incorrect id", exception.getMessage());
		verify(bookingRepository, never()).save(any());
		verifyNoInteractions(slotService);
	}

	@Test
	void shouldThrowWhenBookingNotFound() {
		Long bookingId = 1L;
		ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);

		Booking booking = new Booking(client, specialist, slot, reservation, now);
		ReflectionTestUtils.setField(booking, "id", bookingId);

		when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

		BookingNotFoundException exception = assertThrows(BookingNotFoundException.class,
				() -> bookingService.cancelBooking(bookingId));

		assertEquals("BOOKING_NOT_FOUND", exception.code());
		assertEquals("Booking not found", exception.getMessage());
		verify(bookingRepository, never()).save(any());
		verifyNoInteractions(slotService);
	}

	@Test
	void shouldNotReleaseSlotWhenCancelThrowsException() {
		Long bookingId = 1L;
		ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);
		Booking booking = new Booking(client, specialist, slot, reservation, now);
		ReflectionTestUtils.setField(booking, "id", bookingId);
		booking.complete();

		when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

		InvalidBookingStateException exception = assertThrows(InvalidBookingStateException.class,
				() -> bookingService.cancelBooking(bookingId));

		assertEquals("BOOKING_INVALID_STATE", exception.code());
		assertEquals("Cannot cancel booking", exception.getMessage());
		verifyNoInteractions(slotService);
	}

	@Test
	void shouldReturnAllClientBookingsWhenStatusIsNull() {
		Long clientId = 1L;
		BookingStatus status = null;
		ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);

		Booking completedBooking = new Booking(client, specialist, slot, reservation, now);
		ReflectionTestUtils.setField(completedBooking, "id", 1L);
		completedBooking.complete();

		Booking cancelledBooking = new Booking(client, specialist, slot, reservation, now);
		ReflectionTestUtils.setField(cancelledBooking, "id", 2L);
		cancelledBooking.cancel(now.plusMinutes(1));

		List<Booking> bookings = List.of(completedBooking, cancelledBooking);

		when(bookingRepository.findByClient_Id(clientId)).thenReturn(bookings);

		List<Booking> result = bookingService.getClientBookings(clientId, status);

		assertEquals(bookings, result);

		verify(bookingRepository).findByClient_Id(clientId);
		verify(bookingRepository, never()).findByClient_IdAndStatus(anyLong(), any());
	}

	@Test
	void shouldFilterClientBookingsByStatus() {
		Long clientId = 1L;
		BookingStatus status = BookingStatus.CANCELLED;
		ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);

		Booking cancelledBooking = new Booking(client, specialist, slot, reservation, now);
		ReflectionTestUtils.setField(cancelledBooking, "id", 1L);
		cancelledBooking.cancel(now.plusMinutes(1));

		when(bookingRepository.findByClient_IdAndStatus(clientId, status)).thenReturn(List.of(cancelledBooking));

		List<Booking> result = bookingService.getClientBookings(clientId, status);

		assertEquals(List.of(cancelledBooking), result);

		verify(bookingRepository).findByClient_IdAndStatus(clientId, status);
		verify(bookingRepository, never()).findByClient_Id(clientId);
	}

	@Test
	void shouldThrowWhenGetClientBookingsClientIdIsNull() {
		Long clientId = null;
		BookingStatus status = BookingStatus.CANCELLED;

		InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
				() -> bookingService.getClientBookings(clientId, status));

		assertEquals("USER_INVALID_DATA", exception.code());
		assertEquals("Incorrect client id", exception.getMessage());
		verifyNoInteractions(bookingRepository);
	}

	@Test
	void shouldReturnAllSpecialistBookingsWhenStatusIsNull() {
		Long specialistId = 1L;
		BookingStatus status = null;
		ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);

		Booking completedBooking = new Booking(client, specialist, slot, reservation, now);
		ReflectionTestUtils.setField(completedBooking, "id", 1L);
		completedBooking.complete();

		Booking cancelledBooking = new Booking(client, specialist, slot, reservation, now);
		ReflectionTestUtils.setField(cancelledBooking, "id", 2L);
		cancelledBooking.cancel(now.plusMinutes(1));

		List<Booking> bookings = List.of(completedBooking, cancelledBooking);

		when(bookingRepository.findBySpecialist_Id(specialistId)).thenReturn(bookings);

		List<Booking> result = bookingService.getSpecialistBookings(specialistId, status);

		assertEquals(bookings, result);

		verify(bookingRepository).findBySpecialist_Id(specialistId);
		verify(bookingRepository, never()).findBySpecialist_IdAndStatus(specialistId, status);
	}

	@Test
	void shouldFilterSpecialistBookingsByStatus() {
		Long specialistId = 1L;
		BookingStatus status = BookingStatus.CANCELLED;
		ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);

		Booking cancelledBooking = new Booking(client, specialist, slot, reservation, now);
		ReflectionTestUtils.setField(cancelledBooking, "id", 1L);
		cancelledBooking.cancel(now.plusMinutes(1));

		when(bookingRepository.findBySpecialist_IdAndStatus(specialistId, status))
				.thenReturn(List.of(cancelledBooking));

		List<Booking> result = bookingService.getSpecialistBookings(specialistId, status);

		assertEquals(List.of(cancelledBooking), result);

		verify(bookingRepository).findBySpecialist_IdAndStatus(specialistId, status);
		verify(bookingRepository, never()).findBySpecialist_Id(specialistId);
	}

	@Test
	void shouldThrowWhenGetSpecialistBookingsSpecialistIdIsNull() {
		Long specialistId = null;
		BookingStatus status = BookingStatus.CANCELLED;

		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class,
				() -> bookingService.getSpecialistBookings(specialistId, status));

		assertEquals("SPECIALIST_INVALID_DATA", exception.code());
		assertEquals("Incorrect specialist id", exception.getMessage());
		verifyNoInteractions(bookingRepository);
	}
}
