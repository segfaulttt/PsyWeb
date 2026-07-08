package com.psyweb.availability.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.specialist.domain.Specialist;
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

    @Mock
    private AvailabilitySlotRepository slotRepository;

    @BeforeEach
    void setUp() {
    	slotService = new AvailabilitySlotService(slotRepository);

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
            "bio"
        );
    	
    	LocalDateTime start = LocalDateTime.of(2026,7,10,10,0);
    	LocalDateTime end = LocalDateTime.of(2026,7,10,11,0);
        slot = new AvailabilitySlot(specialist, start, end);
    }

    @Test
    void shouldCreateSlot() {
    	Specialist specialist = mock(Specialist.class);

    	when(specialist.getId())
    	    .thenReturn(1L);
    	LocalDateTime start = LocalDateTime.of(2026,7,10,10,0);
    	LocalDateTime end = LocalDateTime.of(2026,7,10,11,0);
    	
    	when(slotRepository.existsOverlappingSlot(1L, start, end))
    		.thenReturn(false);
    	when(slotRepository.save(any(AvailabilitySlot.class)))
    		.thenAnswer(invocation -> invocation.getArgument(0));
    	ArgumentCaptor<AvailabilitySlot> captor = ArgumentCaptor.forClass(AvailabilitySlot.class);
    	AvailabilitySlot result = slotService.createSlot(specialist, start, end);
    	
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
    void shouldRejectInvalidTime() {
    	LocalDateTime start = LocalDateTime.of(2026,7,10,10,0);
    	LocalDateTime end = LocalDateTime.of(2026,7,10,9,0);
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> slotService.createSlot(specialist, start, end));
    	
    	assertEquals("Start must be before end", exception.getMessage());
    }
    
    @Test
    void shouldBlockFreeSlot() {    	
    	assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
    	
    	when(slotRepository.findById(SLOT_ID))
    		.thenReturn(Optional.of(slot));
    	when(slotRepository.save(any(AvailabilitySlot.class)))
        	.thenAnswer(invocation -> invocation.getArgument(0));
    	
    	AvailabilitySlot result = slotService.blockSlot(SLOT_ID);
    	
    	assertEquals(AvailabilityStatus.BLOCKED, result.getAvailabilityStatus());
    	verify(slotRepository).save(slot);
    }

    @Test
    void shouldRejectBlockBookedSlot() {
    	slot.markBooked();
    	
    	assertEquals(AvailabilityStatus.BOOKED, slot.getAvailabilityStatus());
    	
    	when(slotRepository.findById(SLOT_ID))
        	.thenReturn(Optional.of(slot));
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> slotService.blockSlot(SLOT_ID));
    	
    	assertEquals("Cannot block slot", exception.getMessage());
    	verify(slotRepository, never()).save(any());
    }
    
    @Test
    void shouldFreeBlockedSlot() {
    	slot.markBlocked();
    	assertEquals(AvailabilityStatus.BLOCKED, slot.getAvailabilityStatus());
    	
    	when(slotRepository.findById(SLOT_ID))
    		.thenReturn(Optional.of(slot));
    	when(slotRepository.save(any(AvailabilitySlot.class)))
        	.thenAnswer(invocation -> invocation.getArgument(0));
    	
    	AvailabilitySlot result = slotService.freeSlot(SLOT_ID);
    	
    	assertEquals(AvailabilityStatus.FREE, result.getAvailabilityStatus());
    	verify(slotRepository).save(slot);
    }
    
    @Test
    void shouldRejectFreeSlotWithInvalidStatus() {
    	assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());

    	when(slotRepository.findById(SLOT_ID))
    		.thenReturn(Optional.of(slot));
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> slotService.freeSlot(SLOT_ID));
    	
    	assertEquals("Cannot free slot", exception.getMessage());
    	verify(slotRepository, never()).save(any());
    }
    
    @Test
    void shouldFreeBookedSlot() {
    	slot.markBooked();
    	assertEquals(AvailabilityStatus.BOOKED, slot.getAvailabilityStatus());
    	
    	when(slotRepository.findById(SLOT_ID))
    		.thenReturn(Optional.of(slot));
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> slotService.freeSlot(SLOT_ID));
    	assertEquals("Cannot free slot", exception.getMessage());
    	verify(slotRepository, never()).save(any());
    }
    
    @Test
    void shouldRejectSlotCreationWhenOverlapExists() {
    	LocalDateTime start = LocalDateTime.of(2026,7,10,10,0);
    	LocalDateTime end = LocalDateTime.of(2026,7,10,11,0);

    	Specialist specialist = mock(Specialist.class);

    	when(specialist.getId())
    	    .thenReturn(1L);
    	
    	when(slotRepository.existsOverlappingSlot(1L, start, end))
    		.thenReturn(true);
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> slotService.createSlot(specialist, start, end));
    	
    	assertEquals("Overlap", exception.getMessage());
    	verify(slotRepository, never()).save(any());
    	verify(slotRepository).existsOverlappingSlot(1L, start, end);
    }
}