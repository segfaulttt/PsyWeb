package com.psyweb.availability.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.exception.AvailabilitySlotNotFoundException;
import com.psyweb.availability.exception.InvalidAvailabilitySlotDataException;
import com.psyweb.availability.exception.SlotOverlapException;
import com.psyweb.availability.exception.InvalidAvailabilitySlotStateException;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.exception.SpecialistNotEligibleException;
import com.psyweb.specialist.service.SpecialistService;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class AvailabilitySlotServiceTest {

	private AvailabilitySlot slot;
	private Specialist specialist;
	private AvailabilitySlotService slotService;
	private static final Long SLOT_ID = 1L;
	private final Clock clock = Clock.fixed(Instant.parse("2099-01-01T10:00:00Z"), ZoneId.of("UTC"));
	private final Instant now = clock.instant();

	@Mock
	private AvailabilitySlotRepository slotRepository;

	@Mock
	private SpecialistService specialistService;

	@BeforeEach
	void setUp() {
		slotService = new AvailabilitySlotService(slotRepository, specialistService, clock);

		User user = new User("email@gmail.com", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);

		specialist = new Specialist(user, "firstName", "lastName", Duration.ZERO, Duration.ZERO);

		Instant start = now.plus(Duration.ofHours(1));
		Instant end = now.plus(Duration.ofHours(2));
		slot = new AvailabilitySlot(specialist, start, end);
	}

	@Test
	public void shouldCreateSlot() {
		Specialist specialist = mock(Specialist.class);

		when(specialist.getId()).thenReturn(1L);
		when(specialistService.getEligibleSpecialistForUpdate(specialist.getId())).thenReturn(specialist);
		Instant start = now.plus(Duration.ofHours(1));
		Instant end = now.plus(Duration.ofHours(2));

		when(slotRepository.existsOverlappingSlot(1L, start, end)).thenReturn(false);
		when(slotRepository.saveAndFlush(any(AvailabilitySlot.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<AvailabilitySlot> captor = ArgumentCaptor.forClass(AvailabilitySlot.class);
		AvailabilitySlot result = slotService.createSlot(specialist.getId(), start, end);

		assertEquals(specialist.getId(), result.getSpecialistId());
		assertEquals(AvailabilityStatus.FREE, result.getAvailabilityStatus());
		assertEquals(start, result.getStartTime());
		assertEquals(end, result.getEndTime());

		verify(slotRepository).saveAndFlush(captor.capture());
		AvailabilitySlot captured = captor.getValue();
		assertEquals(start, captured.getStartTime());
		assertEquals(end, captured.getEndTime());
		verify(slotRepository).existsOverlappingSlot(1L, start, end);
	}

	@Test
	public void shouldRejectInvalidTime() {
		Instant start = now.plus(Duration.ofMinutes(10));
		Instant end = now.plus(Duration.ofMinutes(5));

		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> slotService.createSlot(1L, start, end));

		assertEquals("AVAILABILITY_SLOT_INVALID_DATA", exception.code());
		assertEquals("Start must be before end", exception.getMessage());
	}

	@Test
	public void shouldReserveFreeSlot() {
		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		when(slotRepository.save(any(AvailabilitySlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AvailabilitySlot result = slotService.reserveSlot(SLOT_ID);

		assertEquals(AvailabilityStatus.RESERVED, result.getAvailabilityStatus());
		verify(slotRepository).save(slot);
	}

	@Test
	public void shouldRejectReservationWhenSlotIsBooked() {
		slot.reserve();
		slot.confirmBooking();

		assertEquals(AvailabilityStatus.BOOKED, slot.getAvailabilityStatus());

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slotService.reserveSlot(SLOT_ID));

		assertEquals("Cannot reserve slot with status BOOKED", exception.getMessage());
		verify(slotRepository, never()).save(any());
	}

	@Test
	public void shouldRejectReservationReleaseWhenSlotIsFree() {
		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slotService.releaseReservation(SLOT_ID));

		assertEquals("Cannot release reservation from slot with status FREE", exception.getMessage());
		verify(slotRepository, never()).save(any());
	}

	@Test
	public void shouldRejectSlotCreationWhenOverlapExists() {
		Instant start = now.plus(Duration.ofHours(1));
		Instant end = now.plus(Duration.ofHours(2));

		when(specialistService.getEligibleSpecialistForUpdate(1L)).thenReturn(specialist);
		when(slotRepository.existsOverlappingSlot(1L, start, end)).thenReturn(true);

		SlotOverlapException exception = assertThrows(SlotOverlapException.class,
				() -> slotService.createSlot(1L, start, end));

		assertEquals("SLOT_OVERLAP", exception.code());
		assertEquals("Slot overlap", exception.getMessage());
		verify(slotRepository, never()).saveAndFlush(any());
		verify(slotRepository).existsOverlappingSlot(1L, start, end);
	}

	@Test
	public void shouldRejectSlotCreationWhenSpecialistIdIsNull() {
		Instant start = now.plus(Duration.ofHours(1));
		Instant end = now.plus(Duration.ofHours(2));

		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> slotService.createSlot(null, start, end));

		assertEquals("AVAILABILITY_SLOT_INVALID_DATA", exception.code());
		assertEquals("Specialist id cannot be null", exception.getMessage());

		verify(specialistService, never()).getEligibleSpecialistForUpdate(any());
		verify(slotRepository, never()).save(any());
		verify(slotRepository, never()).existsOverlappingSlot(1L, start, end);
	}

	@Test
	public void shouldRejectSlotCreationWhenStartTimeIsNull() {
		Instant end = now.plus(Duration.ofHours(2));

		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> slotService.createSlot(1L, null, end));

		assertEquals("AVAILABILITY_SLOT_INVALID_DATA", exception.code());
		assertEquals("Time cannot be null", exception.getMessage());

		verify(specialistService, never()).getEligibleSpecialistForUpdate(any());
		verify(slotRepository, never()).save(any());
		verify(slotRepository, never()).existsOverlappingSlot(any(), any(), any());
	}

	@Test
	public void shouldRejectSlotCreationWhenEndTimeIsNull() {
		Instant start = now.plus(Duration.ofHours(1));

		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> slotService.createSlot(1L, start, null));

		assertEquals("AVAILABILITY_SLOT_INVALID_DATA", exception.code());
		assertEquals("Time cannot be null", exception.getMessage());

		verify(specialistService, never()).getEligibleSpecialistForUpdate(any());
		verify(slotRepository, never()).save(any());
		verify(slotRepository, never()).existsOverlappingSlot(any(), any(), any());
	}

	@Test
	public void shouldRejectSlotCreationWhenStartEqualsEnd() {
		Instant start = now.plus(Duration.ofHours(1));
		Instant end = now.plus(Duration.ofHours(1));

		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> slotService.createSlot(1L, start, end));

		assertEquals("AVAILABILITY_SLOT_INVALID_DATA", exception.code());
		assertEquals("Start must be before end", exception.getMessage());

		verify(specialistService, never()).getEligibleSpecialistForUpdate(any());
		verify(slotRepository, never()).save(any());
		verify(slotRepository, never()).existsOverlappingSlot(1L, start, end);
	}

	@Test
	public void shouldRejectSlotCreationWhenStartTimeIsInPast() {
		Instant start = now.minus(Duration.ofMinutes(5));
		Instant end = now.plus(Duration.ofHours(1));

		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> slotService.createSlot(1L, start, end));

		assertEquals("AVAILABILITY_SLOT_INVALID_DATA", exception.code());
		assertEquals("Start time must be after now", exception.getMessage());

		verify(specialistService, never()).getEligibleSpecialistForUpdate(any());
		verify(slotRepository, never()).save(any());
		verify(slotRepository, never()).existsOverlappingSlot(1L, start, end);
	}

	@Test
	public void shouldRejectSlotCreationWhenStartTimeEqualsNow() {
		Instant end = now.plus(Duration.ofHours(1));

		InvalidAvailabilitySlotDataException exception = assertThrows(InvalidAvailabilitySlotDataException.class,
				() -> slotService.createSlot(1L, now, end));

		assertEquals("AVAILABILITY_SLOT_INVALID_DATA", exception.code());
		assertEquals("Start time must be after now", exception.getMessage());

		verify(specialistService, never()).getEligibleSpecialistForUpdate(any());
		verify(slotRepository, never()).save(any());
		verify(slotRepository, never()).existsOverlappingSlot(1L, now, end);
	}

	@Test
	public void shouldNotSaveSlotWhenSpecialistValidationFails() {
		Specialist specialist = mock(Specialist.class);

		Instant start = now.plus(Duration.ofHours(1));
		Instant end = now.plus(Duration.ofHours(2));

		when(specialist.getId()).thenReturn(1L);
		when(specialistService.getEligibleSpecialistForUpdate(specialist.getId()))
				.thenThrow(new SpecialistNotEligibleException("Specialist must have status 'APPROVED'"));

		SpecialistNotEligibleException exception = assertThrows(SpecialistNotEligibleException.class,
				() -> slotService.createSlot(1L, start, end));

		assertEquals("Specialist must have status 'APPROVED'", exception.getMessage());

		verify(specialistService).getEligibleSpecialistForUpdate(specialist.getId());
		verify(slotRepository, never()).save(any());
		verify(slotRepository, never()).existsOverlappingSlot(1L, start, end);
	}

	@Test
	public void shouldReleaseReservationFromReservedSlot() {
		slot.reserve();

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		when(slotRepository.save(slot)).thenReturn(slot);

		AvailabilitySlot result = slotService.releaseReservation(SLOT_ID);

		assertEquals(AvailabilityStatus.FREE, result.getAvailabilityStatus());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository).save(slot);
	}

	@Test
	public void shouldConfirmBookingForReservedSlot() {
		slot.reserve();

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		when(slotRepository.save(slot)).thenReturn(slot);

		AvailabilitySlot result = slotService.confirmBooking(SLOT_ID);

		assertEquals(AvailabilityStatus.BOOKED, result.getAvailabilityStatus());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository).save(slot);
	}

	@Test
	public void shouldReleaseBookingFromBookedSlot() {
		slot.reserve();
		slot.confirmBooking();

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		when(slotRepository.save(slot)).thenReturn(slot);

		AvailabilitySlot result = slotService.releaseBooking(SLOT_ID);

		assertEquals(AvailabilityStatus.FREE, result.getAvailabilityStatus());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository).save(slot);
	}

	@Test
	public void shouldCancelFreeSlot() {
		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		when(slotRepository.save(slot)).thenReturn(slot);

		AvailabilitySlot result = slotService.cancelSlot(SLOT_ID, now, CancellationInitiator.SPECIALIST,
				CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		assertEquals(AvailabilityStatus.CANCELLED, result.getAvailabilityStatus());
		assertEquals(now, slot.getCancelledAt());
		assertEquals(CancellationInitiator.SPECIALIST, slot.getCancellationInitiator());
		assertEquals(CancellationReason.SPECIALIST_REMOVED_AVAILABILITY, slot.getCancellationReason());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository).save(slot);
	}

	@Test
	public void shouldCancelReservedSlot() {
		slot.reserve();

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		when(slotRepository.save(slot)).thenReturn(slot);

		AvailabilitySlot result = slotService.cancelSlot(SLOT_ID, now, CancellationInitiator.SPECIALIST,
				CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		assertEquals(AvailabilityStatus.CANCELLED, result.getAvailabilityStatus());
		assertEquals(now, slot.getCancelledAt());
		assertEquals(CancellationInitiator.SPECIALIST, slot.getCancellationInitiator());
		assertEquals(CancellationReason.SPECIALIST_REMOVED_AVAILABILITY, slot.getCancellationReason());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository).save(slot);
	}

	@Test
	public void shouldCancelBookedSlot() {
		slot.reserve();
		slot.confirmBooking();

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		when(slotRepository.save(slot)).thenReturn(slot);

		AvailabilitySlot result = slotService.cancelSlot(SLOT_ID, now, CancellationInitiator.SPECIALIST,
				CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		assertEquals(AvailabilityStatus.CANCELLED, result.getAvailabilityStatus());
		assertEquals(now, slot.getCancelledAt());
		assertEquals(CancellationInitiator.SPECIALIST, slot.getCancellationInitiator());
		assertEquals(CancellationReason.SPECIALIST_REMOVED_AVAILABILITY, slot.getCancellationReason());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository).save(slot);
	}

	@Test
	public void shouldRejectRepeatedCancellation() {
		slot.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);
		assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slotService.cancelSlot(SLOT_ID, now, CancellationInitiator.SPECIALIST,
						CancellationReason.SPECIALIST_REMOVED_AVAILABILITY));

		assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());
		assertEquals("Cannot cancel slot with status CANCELLED", exception.getMessage());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository, never()).save(slot);
	}

	@Test
	public void shouldRejectBookingReleaseWhenSlotIsFree() {
		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slotService.releaseBooking(SLOT_ID));

		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
		assertEquals("Cannot release booking from slot with status FREE", exception.getMessage());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository, never()).save(slot);
	}

	@Test
	public void shouldRejectReservationWhenSlotIsReserved() {
		slot.reserve();

		assertEquals(AvailabilityStatus.RESERVED, slot.getAvailabilityStatus());

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slotService.reserveSlot(SLOT_ID));

		assertEquals("Cannot reserve slot with status RESERVED", exception.getMessage());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository, never()).save(any());
	}

	@Test
	public void shouldRejectReservationWhenSlotIsCancelled() {
		slot.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slotService.reserveSlot(SLOT_ID));

		assertEquals("Cannot reserve slot with status CANCELLED", exception.getMessage());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository, never()).save(any());
	}

	@Test
	public void shouldRejectReservationReleaseWhenSlotIsBooked() {
		slot.reserve();
		slot.confirmBooking();

		assertEquals(AvailabilityStatus.BOOKED, slot.getAvailabilityStatus());

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slotService.releaseReservation(SLOT_ID));

		assertEquals("Cannot release reservation from slot with status BOOKED", exception.getMessage());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository, never()).save(any());
	}

	@Test
	public void shouldRejectReservationReleaseWhenSlotIsCancelled() {
		slot.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slotService.releaseReservation(SLOT_ID));

		assertEquals("Cannot release reservation from slot with status CANCELLED", exception.getMessage());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository, never()).save(any());
	}

	@Test
	public void shouldRejectBookingConfirmationWhenSlotIsFree() {
		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slotService.confirmBooking(SLOT_ID));

		assertEquals("Cannot confirm booking for slot with status FREE", exception.getMessage());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository, never()).save(any());
	}

	@Test
	public void shouldRejectBookingConfirmationWhenSlotIsBooked() {
		slot.reserve();
		slot.confirmBooking();

		assertEquals(AvailabilityStatus.BOOKED, slot.getAvailabilityStatus());

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slotService.confirmBooking(SLOT_ID));

		assertEquals("Cannot confirm booking for slot with status BOOKED", exception.getMessage());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository, never()).save(any());
	}

	@Test
	public void shouldRejectBookingConfirmationWhenSlotIsCancelled() {
		slot.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slotService.confirmBooking(SLOT_ID));

		assertEquals("Cannot confirm booking for slot with status CANCELLED", exception.getMessage());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository, never()).save(any());
	}

	@Test
	public void shouldRejectBookingReleaseWhenSlotIsReserved() {
		slot.reserve();

		assertEquals(AvailabilityStatus.RESERVED, slot.getAvailabilityStatus());

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slotService.releaseBooking(SLOT_ID));

		assertEquals("Cannot release booking from slot with status RESERVED", exception.getMessage());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository, never()).save(any());
	}

	@Test
	public void shouldRejectBookingReleaseWhenSlotIsCancelled() {
		slot.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));
		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slotService.releaseBooking(SLOT_ID));

		assertEquals("Cannot release booking from slot with status CANCELLED", exception.getMessage());
		verify(slotRepository).findForUpdateById(SLOT_ID);
		verify(slotRepository, never()).save(any());
	}

	@Test
	void shouldRejectReservationWhenSlotNotFound() {
		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.empty());

		AvailabilitySlotNotFoundException exception = assertThrows(AvailabilitySlotNotFoundException.class,
				() -> slotService.reserveSlot(SLOT_ID));

		assertEquals("AVAILABILITY_SLOT_NOT_FOUND", exception.code());
		assertEquals("Slot not found", exception.getMessage());

		verify(slotRepository, never()).save(any());
	}

	@Test
	public void shouldTranslateSlotOverlapConstraintViolation() {
		Instant start = now.plus(Duration.ofHours(1));
		Instant end = now.plus(Duration.ofHours(2));

		when(specialistService.getEligibleSpecialistForUpdate(1L)).thenReturn(specialist);
		when(slotRepository.existsOverlappingSlot(1L, start, end)).thenReturn(false);

		ConstraintViolationException constraintException = mock(ConstraintViolationException.class);
		when(constraintException.getConstraintName()).thenReturn("no_overlapping_active_slots");

		DataIntegrityViolationException dataException = new DataIntegrityViolationException("Constraint violation",
				constraintException);

		when(slotRepository.saveAndFlush(any(AvailabilitySlot.class))).thenThrow(dataException);

		SlotOverlapException exception = assertThrows(SlotOverlapException.class,
				() -> slotService.createSlot(1L, start, end));

		assertEquals("SLOT_OVERLAP", exception.code());
		assertEquals("Slot overlap", exception.getMessage());
		assertSame(dataException, exception.getCause());

		verify(slotRepository).existsOverlappingSlot(1L, start, end);
		verify(slotRepository).saveAndFlush(any(AvailabilitySlot.class));
	}

	@Test
	public void shouldRethrowUnrelatedDataIntegrityViolation() {
		Instant start = now.plus(Duration.ofHours(1));
		Instant end = now.plus(Duration.ofHours(2));

		when(specialistService.getEligibleSpecialistForUpdate(1L)).thenReturn(specialist);
		when(slotRepository.existsOverlappingSlot(1L, start, end)).thenReturn(false);

		ConstraintViolationException constraintException = mock(ConstraintViolationException.class);
		when(constraintException.getConstraintName()).thenReturn("some_other_constraint");

		DataIntegrityViolationException dataException = new DataIntegrityViolationException("Constraint violation",
				constraintException);

		when(slotRepository.saveAndFlush(any(AvailabilitySlot.class))).thenThrow(dataException);

		DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
				() -> slotService.createSlot(1L, start, end));

		assertSame(dataException, exception);
	}

	@Test
	void shouldRejectReservationAtBookingDeadline() {
		User user = new User("specialist-deadline@example.com", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);

		Specialist specialist = new Specialist(user, "Anna", "Deadline", Duration.ofHours(1), Duration.ZERO);

		AvailabilitySlot slot = new AvailabilitySlot(specialist, now.plus(Duration.ofHours(1)), now.plus(Duration.ofHours(2)));

		// start = now + 1h
		// notice = 1h
		// bookingDeadline = now

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slotService.reserveSlot(SLOT_ID));

		assertEquals("Slot is no longer bookable", exception.getMessage());

		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());

		verify(slotRepository, never()).save(any());
	}

	@Test
	void shouldRejectReservationAfterBookingDeadline() {
		User user = new User("specialist-after-deadline@example.com", "password", UserRole.SPECIALIST,
				UserStatus.ACTIVE);

		Specialist specialist = new Specialist(user, "Anna", "AfterDeadline", Duration.ofHours(2), Duration.ZERO);

		AvailabilitySlot slot = new AvailabilitySlot(specialist, now.plus(Duration.ofHours(1)), now.plus(Duration.ofHours(2)));

		// start = now + 1h
		// notice = 2h
		// deadline = now - 1h

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> slotService.reserveSlot(SLOT_ID));

		assertEquals("Slot is no longer bookable", exception.getMessage());

		assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());

		verify(slotRepository, never()).save(any());
	}

	@Test
	void shouldUseSlotOverrideWhenCheckingBookingDeadline() {
		User user = new User("specialist-service-override@example.com", "password", UserRole.SPECIALIST,
				UserStatus.ACTIVE);

		Specialist specialist = new Specialist(user, "Anna", "Override", Duration.ofHours(2), Duration.ZERO);

		AvailabilitySlot slot = new AvailabilitySlot(specialist, now.plus(Duration.ofHours(1)), now.plus(Duration.ofHours(2)),
				Duration.ofMinutes(30));

		// Specialist default:
		// deadline = now - 1h это уже поздно
		//
		// Slot override:
		// deadline = now + 30m бронировать можно

		when(slotRepository.findForUpdateById(SLOT_ID)).thenReturn(Optional.of(slot));

		when(slotRepository.save(slot)).thenReturn(slot);

		AvailabilitySlot result = slotService.reserveSlot(SLOT_ID);

		assertEquals(AvailabilityStatus.RESERVED, result.getAvailabilityStatus());

		verify(slotRepository).save(slot);
	}
}