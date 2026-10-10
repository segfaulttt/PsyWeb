package com.psyweb.booking.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.service.AvailabilitySlotService;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

@ExtendWith(MockitoExtension.class)
public class SchedulingServiceTest {

	private static final Long SLOT_ID = 10L;
	private static final Long SPECIALIST_ID = 20L;

	private final Clock clock = Clock.fixed(Instant.parse("2099-01-01T10:00:00Z"), ZoneId.of("UTC"));

	private final Instant now = clock.instant();

	@Mock
	private AvailabilitySlotService slotService;

	@Mock
	private BookingService bookingService;

	@Mock
	private ReservationService reservationService;

	private SchedulingService schedulingService;

	@BeforeEach
	void setUp() {
		schedulingService = new SchedulingService(slotService, bookingService, reservationService, clock);
	}

	@Test
	void shouldCancelFutureFreeSlotWhenSpecialistIsSuspended() {
		AvailabilitySlot slot = createSlot(AvailabilityStatus.FREE);

		when(slotService.findFutureSlotsForUpdate(SPECIALIST_ID, now)).thenReturn(List.of(slot));

		schedulingService.cancelFutureSlotsForSpecialistSuspension(SPECIALIST_ID, now);

		verify(slotService).findFutureSlotsForUpdate(SPECIALIST_ID, now);

		verify(slotService).cancelSlot(SLOT_ID, now, CancellationInitiator.ADMIN,
				CancellationReason.SPECIALIST_SUSPENDED);

		verifyNoInteractions(reservationService);
		verifyNoInteractions(bookingService);
	}

	@Test
	void shouldCancelActiveReservationWhenSpecialistIsSuspended() {
		AvailabilitySlot slot = createSlot(AvailabilityStatus.RESERVED);

		when(slotService.findFutureSlotsForUpdate(SPECIALIST_ID, now)).thenReturn(List.of(slot));

		schedulingService.cancelFutureSlotsForSpecialistSuspension(SPECIALIST_ID, now);

		verify(slotService).findFutureSlotsForUpdate(SPECIALIST_ID, now);

		verify(reservationService).cancelActiveReservationForSpecialistSuspension(SLOT_ID, now);

		verify(slotService).cancelSlot(SLOT_ID, now, CancellationInitiator.ADMIN,
				CancellationReason.SPECIALIST_SUSPENDED);

		verifyNoInteractions(bookingService);
	}

	@Test
	void shouldCancelConfirmedBookingWhenSpecialistIsSuspended() {
		AvailabilitySlot slot = createSlot(AvailabilityStatus.BOOKED);

		when(slotService.findFutureSlotsForUpdate(SPECIALIST_ID, now)).thenReturn(List.of(slot));

		schedulingService.cancelFutureSlotsForSpecialistSuspension(SPECIALIST_ID, now);

		verify(slotService).findFutureSlotsForUpdate(SPECIALIST_ID, now);

		verify(bookingService).cancelConfirmedBookingForSpecialistSuspension(SLOT_ID, now);

		verify(slotService).cancelSlot(SLOT_ID, now, CancellationInitiator.ADMIN,
				CancellationReason.SPECIALIST_SUSPENDED);

		verifyNoInteractions(reservationService);
	}

	@Test
	void shouldRemoveFreeSlot() {
		AvailabilitySlot slot = createSlot(AvailabilityStatus.FREE);

		when(slotService.findSlotForUpdate(SLOT_ID)).thenReturn(slot);

		schedulingService.removeSlotBySpecialist(SLOT_ID, SPECIALIST_ID);

		verify(slotService).cancelSlot(SLOT_ID, now, CancellationInitiator.SPECIALIST,
				CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		verifyNoInteractions(reservationService);
		verifyNoInteractions(bookingService);
	}

	@Test
	void shouldCancelActiveReservationWhenRemovingReservedSlot() {
		AvailabilitySlot slot = createSlot(AvailabilityStatus.RESERVED);

		when(slotService.findSlotForUpdate(SLOT_ID)).thenReturn(slot);

		schedulingService.removeSlotBySpecialist(SLOT_ID, SPECIALIST_ID);

		verify(reservationService).cancelActiveReservationForSlotRemoval(SLOT_ID, now);

		verify(slotService).cancelSlot(SLOT_ID, now, CancellationInitiator.SPECIALIST,
				CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		verifyNoInteractions(bookingService);
	}

	@Test
	void shouldCancelConfirmedBookingWhenRemovingBookedSlot() {
		AvailabilitySlot slot = createSlot(AvailabilityStatus.BOOKED);

		when(slotService.findSlotForUpdate(SLOT_ID)).thenReturn(slot);

		schedulingService.removeSlotBySpecialist(SLOT_ID, SPECIALIST_ID);

		verify(bookingService).cancelConfirmedBookingForSlotRemoval(SLOT_ID, now);

		verify(reservationService, never()).cancelActiveReservationForSlotRemoval(SLOT_ID, now);

		verify(slotService).cancelSlot(SLOT_ID, now, CancellationInitiator.SPECIALIST,
				CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);
	}

	private AvailabilitySlot createSlot(AvailabilityStatus status) {
		User specialistUser = new User("specialist@example.com", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);

		Specialist specialist = new Specialist(specialistUser, "Anna", "Smith", Duration.ZERO, Duration.ZERO);

		ReflectionTestUtils.setField(specialist, "id", SPECIALIST_ID);

		AvailabilitySlot slot = new AvailabilitySlot(specialist, now.plus(Duration.ofDays(1)), now.plus(Duration.ofDays(1)).plus(Duration.ofHours(1)));

		ReflectionTestUtils.setField(slot, "id", SLOT_ID);

		if (status == AvailabilityStatus.RESERVED) {
			slot.reserve();
		}

		if (status == AvailabilityStatus.BOOKED) {
			slot.reserve();
			slot.confirmBooking();
		}

		return slot;
	}
}