package com.psyweb.availability.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.psyweb.availability.exception.InvalidAvailabilitySlotDataException;
import com.psyweb.availability.exception.InvalidAvailabilitySlotStateException;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

@ExtendWith(MockitoExtension.class)
public class AvailabilitySlotTest {
	private AvailabilitySlot slot;

	private final Clock clock = Clock.fixed(Instant.parse("2099-01-01T10:00:00Z"), ZoneId.of("UTC"));
	private final Instant now = clock.instant();

	@BeforeEach
	void setUp() {
		User user = new User("email@gmail.com", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);

		Specialist specialist = new Specialist(user, "firstName", "lastName", Duration.ZERO, Duration.ZERO);

		Instant start = now.plus(Duration.ofHours(1));
		Instant end = now.plus(Duration.ofHours(2));
		slot = new AvailabilitySlot(specialist, start, end);
	}

	@Test
	public void shouldRejectCreationWithoutSpecialist() {
		Instant start = now.plus(Duration.ofHours(1));
		Instant end = now.plus(Duration.ofHours(2));
		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> new AvailabilitySlot(null, start, end));
		assertEquals("AVAILABILITY_SLOT_INVALID_DATA", exception.code());
		assertEquals("Specialist cannot be null", exception.getMessage());
	}

	@Test
	public void shouldRejectCreationWhenStartTimeIsNull() {
		Specialist specialist = mock(Specialist.class);
		Instant end = now.plus(Duration.ofHours(2));
		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> new AvailabilitySlot(specialist, null, end));

		assertEquals("AVAILABILITY_SLOT_INVALID_DATA", exception.code());
		assertEquals("Time cannot be null", exception.getMessage());
	}

	@Test
	public void shouldRejectCreationWhenEndTimeIsNull() {
		Specialist specialist = mock(Specialist.class);
		Instant start = now.plus(Duration.ofHours(1));
		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> new AvailabilitySlot(specialist, start, null));

		assertEquals("AVAILABILITY_SLOT_INVALID_DATA", exception.code());
		assertEquals("Time cannot be null", exception.getMessage());
	}

	@Test
	public void shouldRejectCreationWhenStartEqualsEnd() {
		Specialist specialist = mock(Specialist.class);
		Instant start = now.plus(Duration.ofHours(1));
		Instant end = now.plus(Duration.ofHours(1));
		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> new AvailabilitySlot(specialist, start, end));

		assertEquals("AVAILABILITY_SLOT_INVALID_DATA", exception.code());
		assertEquals("Start must be before end", exception.getMessage());
	}

	@Test
	public void shouldRejectCreationWhenStartIsAfterEnd() {
		Specialist specialist = mock(Specialist.class);
		Instant start = now.plus(Duration.ofHours(2));
		Instant end = now.plus(Duration.ofHours(1));
		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> new AvailabilitySlot(specialist, start, end));

		assertEquals("AVAILABILITY_SLOT_INVALID_DATA", exception.code());
		assertEquals("Start must be before end", exception.getMessage());
	}

	@Test
	public void shouldReserveFreeSlot() {
		slot.reserve();

		assertEquals(AvailabilityStatus.RESERVED, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldReleaseReservationFromReservedSlot() {
		slot.reserve();
		assertEquals(AvailabilityStatus.RESERVED, slot.getAvailabilityStatus());

		slot.releaseReservation();

		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldConfirmBookingForReservedSlot() {
		slot.reserve();
		assertEquals(AvailabilityStatus.RESERVED, slot.getAvailabilityStatus());

		slot.confirmBooking();

		assertEquals(AvailabilityStatus.BOOKED, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldReleaseBookingFromBookedSlot() {
		slot.reserve();
		slot.confirmBooking();
		assertEquals(AvailabilityStatus.BOOKED, slot.getAvailabilityStatus());

		slot.releaseBooking();

		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldCancelFreeSlot() {
		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());

		slot.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());
		assertEquals(now, slot.getCancelledAt());
		assertEquals(CancellationInitiator.SPECIALIST, slot.getCancellationInitiator());
		assertEquals(CancellationReason.SPECIALIST_REMOVED_AVAILABILITY, slot.getCancellationReason());
	}

	@Test
	public void shouldCancelReservedSlot() {
		slot.reserve();
		assertEquals(AvailabilityStatus.RESERVED, slot.getAvailabilityStatus());

		slot.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());
		assertEquals(now, slot.getCancelledAt());
		assertEquals(CancellationInitiator.SPECIALIST, slot.getCancellationInitiator());
		assertEquals(CancellationReason.SPECIALIST_REMOVED_AVAILABILITY, slot.getCancellationReason());
	}

	@Test
	public void shouldCancelBookedSlot() {
		slot.reserve();
		slot.confirmBooking();
		assertEquals(AvailabilityStatus.BOOKED, slot.getAvailabilityStatus());

		slot.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());
		assertEquals(now, slot.getCancelledAt());
		assertEquals(CancellationInitiator.SPECIALIST, slot.getCancellationInitiator());
		assertEquals(CancellationReason.SPECIALIST_REMOVED_AVAILABILITY, slot.getCancellationReason());
	}

	@Test
	public void shouldRejectReservationWhenSlotIsReserved() {
		slot.reserve();

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slot.reserve());

		assertEquals("Cannot reserve slot with status RESERVED", exception.getMessage());
		assertEquals(AvailabilityStatus.RESERVED, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldRejectReservationWhenSlotIsBooked() {
		slot.reserve();
		slot.confirmBooking();

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slot.reserve());

		assertEquals("Cannot reserve slot with status BOOKED", exception.getMessage());
		assertEquals(AvailabilityStatus.BOOKED, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldRejectReservationWhenSlotIsCancelled() {
		slot.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slot.reserve());

		assertEquals("Cannot reserve slot with status CANCELLED", exception.getMessage());
		assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldRejectReservationReleaseWhenSlotIsFree() {

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slot.releaseReservation());

		assertEquals("Cannot release reservation from slot with status FREE", exception.getMessage());
		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldRejectReservationReleaseWhenSlotIsBooked() {
		slot.reserve();
		slot.confirmBooking();

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slot.releaseReservation());

		assertEquals("Cannot release reservation from slot with status BOOKED", exception.getMessage());
		assertEquals(AvailabilityStatus.BOOKED, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldRejectReservationReleaseWhenSlotIsCancelled() {
		slot.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slot.releaseReservation());

		assertEquals("Cannot release reservation from slot with status CANCELLED", exception.getMessage());
		assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldRejectBookingConfirmationWhenSlotIsFree() {

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slot.confirmBooking());

		assertEquals("Cannot confirm booking for slot with status FREE", exception.getMessage());
		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldRejectBookingConfirmationWhenSlotIsBooked() {
		slot.reserve();
		slot.confirmBooking();

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slot.confirmBooking());

		assertEquals("Cannot confirm booking for slot with status BOOKED", exception.getMessage());
		assertEquals(AvailabilityStatus.BOOKED, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldRejectBookingConfirmationWhenSlotIsCancelled() {
		slot.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slot.confirmBooking());

		assertEquals("Cannot confirm booking for slot with status CANCELLED", exception.getMessage());
		assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldRejectBookingReleaseWhenSlotIsFree() {

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slot.releaseBooking());

		assertEquals("Cannot release booking from slot with status FREE", exception.getMessage());
		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldRejectBookingReleaseWhenSlotIsReserved() {
		slot.reserve();

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slot.releaseBooking());

		assertEquals("Cannot release booking from slot with status RESERVED", exception.getMessage());
		assertEquals(AvailabilityStatus.RESERVED, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldRejectBookingReleaseWhenSlotIsCancelled() {
		slot.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slot.releaseBooking());

		assertEquals("Cannot release booking from slot with status CANCELLED", exception.getMessage());
		assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldRejectRepeatedCancellation() {
		slot.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slot.cancel(now, CancellationInitiator.SPECIALIST,
						CancellationReason.SPECIALIST_REMOVED_AVAILABILITY));

		assertEquals("Cannot cancel slot with status CANCELLED", exception.getMessage());
		assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());
	}

	@Test
	public void shouldRejectCancellationWhenCancelledAtIsNull() {
		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> slot.cancel(null, CancellationInitiator.SPECIALIST,
						CancellationReason.SPECIALIST_REMOVED_AVAILABILITY));

		assertEquals("AVAILABILITY_SLOT_INVALID_DATA", exception.code());
		assertEquals("Cancellation metadata cannot be null", exception.getMessage());
		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
		assertNull(slot.getCancelledAt());
		assertNull(slot.getCancellationInitiator());
		assertNull(slot.getCancellationReason());
	}

	@Test
	public void shouldRejectCancellationWhenInitiatorIsNull() {
		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> slot.cancel(now, null, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY));

		assertEquals("AVAILABILITY_SLOT_INVALID_DATA", exception.code());
		assertEquals("Cancellation metadata cannot be null", exception.getMessage());
		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
		assertNull(slot.getCancelledAt());
		assertNull(slot.getCancellationInitiator());
		assertNull(slot.getCancellationReason());
	}

	@Test
	public void shouldRejectCancellationWhenReasonIsNull() {
		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> slot.cancel(now, CancellationInitiator.SPECIALIST, null));

		assertEquals("AVAILABILITY_SLOT_INVALID_DATA", exception.code());
		assertEquals("Cancellation metadata cannot be null", exception.getMessage());
		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
		assertNull(slot.getCancelledAt());
		assertNull(slot.getCancellationInitiator());
		assertNull(slot.getCancellationReason());
	}

	@Test
	void shouldUseSpecialistMinimumBookingNoticeWhenOverrideIsNull() {
		User user = new User("specialist-default@example.com", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);

		Specialist specialist = new Specialist(user, "Anna", "Default", Duration.ofHours(2), Duration.ZERO);

		Instant start = now.plus(Duration.ofHours(3));

		AvailabilitySlot slot = new AvailabilitySlot(specialist, start, start.plus(Duration.ofHours(1)));

		assertEquals(Duration.ofHours(2), slot.getEffectiveMinimumBookingNotice());

		assertEquals(start.minus(Duration.ofHours(2)), slot.getBookingDeadline());
	}

	@Test
	void shouldUseSlotMinimumBookingNoticeOverride() {
		User user = new User("specialist-override@example.com", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);

		Specialist specialist = new Specialist(user, "Anna", "Override", Duration.ofHours(2), Duration.ZERO);

		Instant start = now.plus(Duration.ofHours(1));

		AvailabilitySlot slot = new AvailabilitySlot(specialist, start, start.plus(Duration.ofHours(1)), Duration.ofMinutes(30));

		assertEquals(Duration.ofMinutes(30), slot.getEffectiveMinimumBookingNotice());

		assertEquals(start.minus(Duration.ofMinutes(30)), slot.getBookingDeadline());
	}

	@Test
	void shouldAllowZeroMinimumBookingNoticeOverride() {
		User user = new User("specialist-zero@example.com", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);

		Specialist specialist = new Specialist(user, "Anna", "Zero", Duration.ofHours(2), Duration.ZERO);

		Instant start = now.plus(Duration.ofHours(1));

		AvailabilitySlot slot = new AvailabilitySlot(specialist, start, start.plus(Duration.ofHours(1)), Duration.ZERO);

		assertEquals(Duration.ZERO, slot.getEffectiveMinimumBookingNotice());

		assertEquals(start, slot.getBookingDeadline());
	}

	@Test
	void shouldRejectNegativeMinimumBookingNoticeOverride() {
		Specialist specialist = mock(Specialist.class);

		Instant start = now.plus(Duration.ofHours(1));

		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> new AvailabilitySlot(specialist, start, start.plus(Duration.ofHours(1)), Duration.ofMinutes(-1)));

		assertEquals("Minimum booking notice override cannot be negative", exception.getMessage());
	}

	@Test
	void shouldRejectMinimumBookingNoticeOverrideWithPartialMinute() {
		Specialist specialist = mock(Specialist.class);

		Instant start = now.plus(Duration.ofHours(1));

		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> new AvailabilitySlot(specialist, start, start.plus(Duration.ofHours(1)), Duration.ofSeconds(30)));

		assertEquals("Minimum booking notice override must contain whole minutes", exception.getMessage());
	}

	@Test
	void shouldRejectMinimumBookingNoticeOverrideOutsideIntegerRange() {
		Specialist specialist = mock(Specialist.class);

		Instant start = now.plus(Duration.ofHours(1));

		Duration tooLarge = Duration.ofMinutes((long) Integer.MAX_VALUE + 1);

		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> new AvailabilitySlot(specialist, start, start.plus(Duration.ofHours(1)), tooLarge));

		assertEquals("Minimum booking notice override exceeds supported range", exception.getMessage());
	}

}
