package com.psyweb.availability.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.exception.InvalidAvailabilitySlotStateException;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
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
    private final Clock clock = Clock.fixed(
	        Instant.parse("2099-01-01T10:00:00Z"),
	        ZoneId.of("UTC"));
	private final LocalDateTime now = LocalDateTime.now(clock);

    @Mock
    private AvailabilitySlotRepository slotRepository;

    @Mock
    private SpecialistService specialistService;

    @BeforeEach
    void setUp() {
    	slotService = new AvailabilitySlotService(slotRepository, specialistService, clock);

        User user = new User(
            "email@gmail.com",
            "password",
            UserRole.SPECIALIST,
            UserStatus.ACTIVE
        );

        specialist = new Specialist(
            user,
            "firstName",
            "lastName",
            Duration.ZERO, 
            Duration.ZERO
        );
    	
    	LocalDateTime start = now.plusHours(1);
    	LocalDateTime end = now.plusHours(2);
        slot = new AvailabilitySlot(specialist, start, end);
    }

    @Test
    public void shouldCreateSlot() {
    	Specialist specialist = mock(Specialist.class);

    	when(specialist.getId())
	    	.thenReturn(1L);
    	when(specialistService.getEligibleSpecialist(specialist.getId()))
    	    .thenReturn(specialist);
    	LocalDateTime start = now.plusHours(1);
    	LocalDateTime end = now.plusHours(2);
    	
    	when(slotRepository.existsOverlappingSlot(1L, start, end))
    		.thenReturn(false);
    	when(slotRepository.save(any(AvailabilitySlot.class)))
    		.thenAnswer(invocation -> invocation.getArgument(0));
    	ArgumentCaptor<AvailabilitySlot> captor = ArgumentCaptor.forClass(AvailabilitySlot.class);
    	AvailabilitySlot result = slotService.createSlot(specialist.getId(), start, end);
    	
    	assertEquals(specialist.getId(), result.getSpecialistId());
    	assertEquals(AvailabilityStatus.FREE, result.getAvailabilityStatus());
    	assertEquals(start, result.getStartTime());
    	assertEquals(end, result.getEndTime());
    	
        verify(slotRepository).save(captor.capture());
        AvailabilitySlot captured = captor.getValue();
        assertEquals(start, captured.getStartTime());
        assertEquals(end, captured.getEndTime());
        verify(slotRepository).existsOverlappingSlot(1L, start, end);
    }
    
    @Test
    public void shouldRejectInvalidTime() {
    	LocalDateTime start = now.plusMinutes(10);
    	LocalDateTime end = now.plusMinutes(5);

    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> slotService.createSlot(1L, start, end));
    	
    	assertEquals("Start must be before end", exception.getMessage());
    }
    
    @Test
    public void shouldReserveFreeSlot() {    	
    	assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
    	
    	when(slotRepository.findById(SLOT_ID))
    		.thenReturn(Optional.of(slot));
    	when(slotRepository.save(any(AvailabilitySlot.class)))
        	.thenAnswer(invocation -> invocation.getArgument(0));
    	
    	AvailabilitySlot result = slotService.reserveSlot(SLOT_ID);
    	
    	assertEquals(AvailabilityStatus.RESERVED, result.getAvailabilityStatus());
    	verify(slotRepository).save(slot);
    }

    @Test
    public void shouldRejectReservationWhenSlotIsBooked() {
    	slot.reserve();
    	slot.confirmBooking();
    	
    	assertEquals(AvailabilityStatus.BOOKED, slot.getAvailabilityStatus());
    	
    	when(slotRepository.findById(SLOT_ID))
        	.thenReturn(Optional.of(slot));
    	InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class, 
    			() -> slotService.reserveSlot(SLOT_ID));
    	
    	assertEquals("Cannot reserve slot with status BOOKED", exception.getMessage());
    	verify(slotRepository, never()).save(any());
    }
    
    @Test
    public void shouldRejectReservationReleaseWhenSlotIsFree() {
    	assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());

    	when(slotRepository.findById(SLOT_ID))
    		.thenReturn(Optional.of(slot));
    	InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class, 
    			() -> slotService.releaseReservation(SLOT_ID));
    	
    	assertEquals("Cannot release reservation from slot with status FREE", exception.getMessage());
    	verify(slotRepository, never()).save(any());
    }
    
    @Test
    public void shouldRejectSlotCreationWhenOverlapExists() {
    	LocalDateTime start = now.plusHours(1);
    	LocalDateTime end = now.plusHours(2);

    	when(specialistService.getEligibleSpecialist(1L))
    		.thenReturn(specialist);
    	when(slotRepository.existsOverlappingSlot(1L, start, end))
    		.thenReturn(true);
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> slotService.createSlot(1L, start, end));
    	
    	assertEquals("Overlap", exception.getMessage());
    	verify(slotRepository, never()).save(any());
    	verify(slotRepository).existsOverlappingSlot(1L, start, end);
    }
    
    @Test
    public void shouldRejectSlotCreationWhenSpecialistIdIsNull() {
    	LocalDateTime start = now.plusHours(1);
    	LocalDateTime end = now.plusHours(2);
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> slotService.createSlot(null, start, end));
    	
    	assertEquals("Specialist id cannot be null", exception.getMessage());
    	
    	verify(specialistService, never()).getEligibleSpecialist(any());
    	verify(slotRepository, never()).save(any());
        verify(slotRepository, never()).existsOverlappingSlot(1L, start, end);
    }
    
    @Test
    public void shouldRejectSlotCreationWhenStartTimeIsNull() {
    	LocalDateTime end = now.plusHours(2);
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> slotService.createSlot(1L, null, end));
    	
    	assertEquals("Time cannot be null", exception.getMessage());
    	
    	verify(specialistService, never()).getEligibleSpecialist(any());
    	verify(slotRepository, never()).save(any());
        verify(slotRepository, never()).existsOverlappingSlot(any(), any(), any());
    }
    
    @Test
    public void shouldRejectSlotCreationWhenEndTimeIsNull() {
    	LocalDateTime start = now.plusHours(1);
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> slotService.createSlot(1L, start, null));
    	
    	assertEquals("Time cannot be null", exception.getMessage());
    	
    	verify(specialistService, never()).getEligibleSpecialist(any());
    	verify(slotRepository, never()).save(any());
        verify(slotRepository, never()).existsOverlappingSlot(any(), any(), any());
    }
    
    @Test
    public void shouldRejectSlotCreationWhenStartEqualsEnd() {
    	LocalDateTime start = now.plusHours(1);
    	LocalDateTime end = now.plusHours(1);
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> slotService.createSlot(1L, start, end));
    	
    	assertEquals("Start must be before end", exception.getMessage());
    	
    	verify(specialistService, never()).getEligibleSpecialist(any());
    	verify(slotRepository, never()).save(any());
        verify(slotRepository, never()).existsOverlappingSlot(1L, start, end);
    }
    
    @Test
    public void shouldRejectSlotCreationWhenStartTimeIsInPast() {
    	LocalDateTime start = now.minusMinutes(5);
    	LocalDateTime end = now.plusHours(1);
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> slotService.createSlot(1L, start, end));
    	
    	assertEquals("Start time must be after now", exception.getMessage());
    	
    	verify(specialistService, never()).getEligibleSpecialist(any());
    	verify(slotRepository, never()).save(any());
        verify(slotRepository, never()).existsOverlappingSlot(1L, start, end);
    }
    
    @Test
    public void shouldRejectSlotCreationWhenStartTimeEqualsNow() {
    	LocalDateTime end = now.plusHours(1);
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> slotService.createSlot(1L, now, end));
    	
    	assertEquals("Start time must be after now", exception.getMessage());
    	
    	verify(specialistService, never()).getEligibleSpecialist(any());
    	verify(slotRepository, never()).save(any());
        verify(slotRepository, never()).existsOverlappingSlot(1L, now, end);
    }
    
    @Test
    public void shouldNotSaveSlotWhenSpecialistValidationFails() {
    	Specialist specialist = mock(Specialist.class);

    	LocalDateTime start = now.plusHours(1);
    	LocalDateTime end = now.plusHours(2);
    	
    	when(specialist.getId())
    		.thenReturn(1L);
    	when(specialistService.getEligibleSpecialist(specialist.getId()))
    		.thenThrow(new SpecialistNotEligibleException("Specialist must have status 'APPROVED'"));
    	
    	
    	SpecialistNotEligibleException exception = assertThrows(SpecialistNotEligibleException.class, 
    			() -> slotService.createSlot(1L, start, end));
    	
    	assertEquals("Specialist must have status 'APPROVED'", exception.getMessage());
    	
    	verify(specialistService).getEligibleSpecialist(specialist.getId());
    	verify(slotRepository, never()).save(any());
        verify(slotRepository, never()).existsOverlappingSlot(1L, start, end);
    }
    
    @Test
    public void shouldReleaseReservationFromReservedSlot() {
    	slot.reserve();
    	
    	when(slotRepository.findById(SLOT_ID))
    		.thenReturn(Optional.of(slot));
    	when(slotRepository.save(slot)).thenReturn(slot);
    	
    	AvailabilitySlot result = slotService.releaseReservation(SLOT_ID);
    	
    	assertEquals(AvailabilityStatus.FREE, result.getAvailabilityStatus());
    	verify(slotRepository).findById(SLOT_ID);
    	verify(slotRepository).save(slot);
    }
    
    @Test
    public void shouldConfirmBookingForReservedSlot() {
    	slot.reserve();
    	
    	when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
    	when(slotRepository.save(slot)).thenReturn(slot);
    	
    	AvailabilitySlot result = slotService.confirmBooking(SLOT_ID);
    	
    	assertEquals(AvailabilityStatus.BOOKED, result.getAvailabilityStatus());
    	verify(slotRepository).findById(SLOT_ID);
    	verify(slotRepository).save(slot);
    }
    
    @Test
    public void shouldReleaseBookingFromBookedSlot() {
    	slot.reserve();
    	slot.confirmBooking();
    	
    	when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
    	when(slotRepository.save(slot)).thenReturn(slot);
    	
    	AvailabilitySlot result = slotService.releaseBooking(SLOT_ID);
    	
    	assertEquals(AvailabilityStatus.FREE, result.getAvailabilityStatus());
    	verify(slotRepository).findById(SLOT_ID);
    	verify(slotRepository).save(slot);
    }
    
    @Test
    public void shouldCancelFreeSlot() {
    	assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
    	
    	when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
    	when(slotRepository.save(slot)).thenReturn(slot);
    	
    	AvailabilitySlot result = slotService.cancelSlot(SLOT_ID);
    	
    	assertEquals(AvailabilityStatus.CANCELLED, result.getAvailabilityStatus());
    	verify(slotRepository).findById(SLOT_ID);
    	verify(slotRepository).save(slot);
    }
    
    @Test
    public void shouldCancelReservedSlot() {
    	slot.reserve();
    	
    	when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
    	when(slotRepository.save(slot)).thenReturn(slot);
    	
    	AvailabilitySlot result = slotService.cancelSlot(SLOT_ID);
    	
    	assertEquals(AvailabilityStatus.CANCELLED, result.getAvailabilityStatus());
    	verify(slotRepository).findById(SLOT_ID);
    	verify(slotRepository).save(slot);
    }
    
    @Test
    public void shouldCancelBookedSlot() {
    	slot.reserve();
    	slot.confirmBooking();
    	
    	when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
    	when(slotRepository.save(slot)).thenReturn(slot);
    	
    	AvailabilitySlot result = slotService.cancelSlot(SLOT_ID);
    	
    	assertEquals(AvailabilityStatus.CANCELLED, result.getAvailabilityStatus());
    	verify(slotRepository).findById(SLOT_ID);
    	verify(slotRepository).save(slot);
    }
    
    @Test
    public void shouldRejectRepeatedCancellation() {
    	slot.cancel();
    	assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());
    	
    	when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
    	
    	InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class, 
    			() -> slotService.cancelSlot(SLOT_ID));
    	
    	assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());
    	assertEquals("Cannot cancel slot with status CANCELLED", exception.getMessage());
    	verify(slotRepository).findById(SLOT_ID);
    	verify(slotRepository, never()).save(slot);
    }
    
    @Test
    public void shouldRejectBookingReleaseWhenSlotIsFree() {
    	when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
    	
    	InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class, 
    			() -> slotService.releaseBooking(SLOT_ID));
    	
    	assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
    	assertEquals("Cannot release booking from slot with status FREE", exception.getMessage());
    	verify(slotRepository).findById(SLOT_ID);
    	verify(slotRepository, never()).save(slot);
    }
}