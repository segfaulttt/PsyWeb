package com.psyweb.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.booking.exception.InvalidReservationDataException;
import com.psyweb.booking.exception.InvalidReservationStateException;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

public class ReservationTest {
	private final Clock clock = Clock.fixed(Instant.parse("2099-01-01T10:00:00Z"), ZoneId.of("UTC"));
	private final LocalDateTime now = LocalDateTime.now(clock);
	private Specialist specialist;
	private User client;
	private AvailabilitySlot slot;
	private Reservation reservation;

	@BeforeEach
	void setUp() {
		client = new User("example@email.ru", "password", UserRole.CLIENT, UserStatus.ACTIVE);

		User user = new User("email@gmail.com", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);

		specialist = new Specialist(user, "firstName", "lastName", Duration.ZERO, Duration.ZERO);

		slot = new AvailabilitySlot(specialist, now, now.plusHours(1));

		reservation = new Reservation(client, slot, now.minusMinutes(10), now);
	}

	@Test
	public void shouldBeExpiredWhenNowEqualsExpiresAt() {
		assertTrue(reservation.isExpired(now));
		assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
	}

	@Test
	public void shouldNotBeExpiredWhenNowIsBeforeExpiresAt() {
		Reservation activeReservation = new Reservation(client, slot, now.minusMinutes(10), now.plusMinutes(1));

		assertFalse(activeReservation.isExpired(now));
		assertEquals(ReservationStatus.ACTIVE, activeReservation.getStatus());
	}

	@Test
	public void shouldRejectConfirmationWhenNowEqualsExpiresAt() {

		InvalidReservationStateException exception = assertThrows(InvalidReservationStateException.class,
				() -> reservation.confirm(now));

		assertEquals("Cannot mark confirm", exception.getMessage());
		assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
	}

	@Test
	public void shouldStoreCancellationMetadataWhenCancelled() {
		reservation.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);
		
		assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
		assertEquals(now, reservation.getCancelledAt());
		assertEquals(CancellationInitiator.SPECIALIST, reservation.getCancellationInitiator());
		assertEquals(CancellationReason.SPECIALIST_REMOVED_AVAILABILITY, reservation.getCancellationReason());
	}

	@Test
	public void shouldRejectCancellationWhenCancelledAtIsNull() {
		InvalidReservationDataException exception = assertThrows(InvalidReservationDataException.class,
				() -> reservation.cancel(null, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY));
		
		assertEquals("RESERVATION_INVALID_DATA", exception.code());
		assertEquals("Cancellation metadata cannot be null", exception.getMessage());
		
		assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
		assertNull(reservation.getCancelledAt());
		assertNull(reservation.getCancellationInitiator());
		assertNull(reservation.getCancellationReason());
	}

	@Test
	public void shouldRejectCancellationWhenInitiatorIsNull() {
		InvalidReservationDataException exception = assertThrows(InvalidReservationDataException.class,
				() -> reservation.cancel(now, null, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY));
		
		assertEquals("RESERVATION_INVALID_DATA", exception.code());
		assertEquals("Cancellation metadata cannot be null", exception.getMessage());
		
		assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
		assertNull(reservation.getCancelledAt());
		assertNull(reservation.getCancellationInitiator());
		assertNull(reservation.getCancellationReason());
	}

	@Test
	public void shouldRejectCancellationWhenReasonIsNull() {
		InvalidReservationDataException exception = assertThrows(InvalidReservationDataException.class,
				() -> reservation.cancel(now, CancellationInitiator.SPECIALIST, null));
		
		assertEquals("RESERVATION_INVALID_DATA", exception.code());
		assertEquals("Cancellation metadata cannot be null", exception.getMessage());
		
		assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
		assertNull(reservation.getCancelledAt());
		assertNull(reservation.getCancellationInitiator());
		assertNull(reservation.getCancellationReason());
	}

	@Test
	public void shouldExpireWithoutCancellationMetadata() {
		reservation.expire(now.plusMinutes(1));
		
		InvalidReservationStateException exception = assertThrows(InvalidReservationStateException.class,
				() -> reservation.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY));
		
		assertEquals("RESERVATION_INVALID_STATE", exception.code());
		assertEquals("Cannot mark cancelled", exception.getMessage());
		
		assertEquals(ReservationStatus.EXPIRED, reservation.getStatus());
		assertNull(reservation.getCancelledAt());
		assertNull(reservation.getCancellationInitiator());
		assertNull(reservation.getCancellationReason());
	}
}
