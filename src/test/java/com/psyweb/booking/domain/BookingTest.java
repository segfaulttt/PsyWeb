package com.psyweb.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.booking.exception.InvalidBookingDataException;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

public class BookingTest {
	private enum GraphMismatch {
		CLIENT, SLOT, SPECIALIST
	}

	private final Clock clock = Clock.fixed(Instant.parse("2099-01-01T10:00:00Z"), ZoneId.of("UTC"));
	private final LocalDateTime now = LocalDateTime.now(clock);
	private Booking booking;

	@BeforeEach
	public void setUp() {
		User client = new User("example@email.ru", "password", UserRole.CLIENT, UserStatus.ACTIVE);
		User spec = new User("email@gmail.com", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);
		Specialist specialist = new Specialist(spec, "firstName", "lastName", Duration.ZERO, Duration.ZERO);
		AvailabilitySlot slot = new AvailabilitySlot(specialist, now, now.plusHours(1));
		Reservation reservation = new Reservation(client, slot, now, now.plusMinutes(2));

		slot.reserve();
		slot.confirmBooking();
		reservation.confirm(now);

		booking = new Booking(client, specialist, slot, reservation, now.minusMinutes(10));

	}

	@Test
	public void shouldStoreCancellationMetadataWhenCancelled() {
		booking.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		assertEquals(BookingStatus.CANCELLED, booking.getStatus());
		assertEquals(now, booking.getCancelledAt());
		assertEquals(CancellationInitiator.SPECIALIST, booking.getCancellationInitiator());
		assertEquals(CancellationReason.SPECIALIST_REMOVED_AVAILABILITY, booking.getCancellationReason());
	}

	@Test
	public void shouldRejectCancellationWhenCancelledAtIsNull() {
		InvalidBookingDataException exception = assertThrows(InvalidBookingDataException.class, () -> booking
				.cancel(null, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY));

		assertEquals("BOOKING_INVALID_DATA", exception.code());
		assertEquals("Cancellation metadata cannot be null", exception.getMessage());
		assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
		assertNull(booking.getCancelledAt());
		assertNull(booking.getCancellationInitiator());
		assertNull(booking.getCancellationReason());
	}

	@Test
	public void shouldRejectCancellationWhenInitiatorIsNull() {
		InvalidBookingDataException exception = assertThrows(InvalidBookingDataException.class,
				() -> booking.cancel(now, null, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY));

		assertEquals("BOOKING_INVALID_DATA", exception.code());
		assertEquals("Cancellation metadata cannot be null", exception.getMessage());
		assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
		assertNull(booking.getCancelledAt());
		assertNull(booking.getCancellationInitiator());
		assertNull(booking.getCancellationReason());
	}

	@Test
	public void shouldRejectCancellationWhenReasonIsNull() {
		InvalidBookingDataException exception = assertThrows(InvalidBookingDataException.class,
				() -> booking.cancel(now, CancellationInitiator.SPECIALIST, null));

		assertEquals("BOOKING_INVALID_DATA", exception.code());
		assertEquals("Cancellation metadata cannot be null", exception.getMessage());
		assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
		assertNull(booking.getCancelledAt());
		assertNull(booking.getCancellationInitiator());
		assertNull(booking.getCancellationReason());
	}

	@ParameterizedTest
	@EnumSource(GraphMismatch.class)
	public void shouldRejectInconsistentBookingGraph(GraphMismatch mismatch) {
		User client = new User("client@example.com", "password", UserRole.CLIENT, UserStatus.ACTIVE);

		User anotherClient = new User("another-client@example.com", "password", UserRole.CLIENT, UserStatus.ACTIVE);

		User specialistUser = new User("specialist@example.com", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);

		User anotherSpecialistUser = new User("another-specialist@example.com", "password", UserRole.SPECIALIST,
				UserStatus.ACTIVE);

		Specialist specialist = new Specialist(specialistUser, "Anna", "Smith", Duration.ZERO, Duration.ZERO);

		Specialist anotherSpecialist = new Specialist(anotherSpecialistUser, "Kate", "Jones", Duration.ZERO,
				Duration.ZERO);

		AvailabilitySlot slot = new AvailabilitySlot(specialist, now, now.plusHours(1));

		AvailabilitySlot anotherSlot = new AvailabilitySlot(specialist, now.plusHours(2), now.plusHours(3));

		ReflectionTestUtils.setField(client, "id", 1L);
		ReflectionTestUtils.setField(anotherClient, "id", 2L);
		ReflectionTestUtils.setField(specialist, "id", 10L);
		ReflectionTestUtils.setField(anotherSpecialist, "id", 11L);
		ReflectionTestUtils.setField(slot, "id", 100L);
		ReflectionTestUtils.setField(anotherSlot, "id", 101L);

		Reservation reservation = new Reservation(client, slot, now, now.plusMinutes(10));

		reservation.confirm(now);

		User bookingClient = client;
		AvailabilitySlot bookingSlot = slot;
		Specialist bookingSpecialist = specialist;
		String expectedMessage = null;

		switch (mismatch) {
		case CLIENT -> {
			bookingClient = anotherClient;
			expectedMessage = "Booking client does not match reservation client";
		}
		case SLOT -> {
			bookingSlot = anotherSlot;
			expectedMessage = "Booking slot does not match reservation slot";
		}
		case SPECIALIST -> {
			bookingSpecialist = anotherSpecialist;
			expectedMessage = "Booking specialist does not match slot specialist";
		}
		}

		User finalClient = bookingClient;
		AvailabilitySlot finalSlot = bookingSlot;
		Specialist finalSpecialist = bookingSpecialist;

		InvalidBookingDataException exception = assertThrows(InvalidBookingDataException.class,
				() -> new Booking(finalClient, finalSpecialist, finalSlot, reservation, now));

		assertEquals("BOOKING_INVALID_DATA", exception.code());
		assertEquals(expectedMessage, exception.getMessage());
	}
}
