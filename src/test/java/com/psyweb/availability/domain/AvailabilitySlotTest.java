package com.psyweb.availability.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.psyweb.specialist.domain.Specialist;

@ExtendWith(MockitoExtension.class)
public class AvailabilitySlotTest {
	
	private final Clock clock = Clock.fixed(
	        Instant.parse("2099-01-01T10:00:00Z"),
	        ZoneId.of("UTC"));
	private final LocalDateTime now = LocalDateTime.now(clock);
	
	@Test
	public void shouldRejectCreationWithoutSpecialist() {
		LocalDateTime start = now.plusHours(1);
    	LocalDateTime end = now.plusHours(2);
		Exception exception = assertThrows(IllegalArgumentException.class, 
				() -> new AvailabilitySlot(null, start, end));
		
		assertEquals("Specialist cannot be null", exception.getMessage());
	}
	
	@Test
	public void shouldRejectCreationWhenStartTimeIsNull() {
		Specialist specialist = mock(Specialist.class);
    	LocalDateTime end = now.plusHours(2);
		Exception exception = assertThrows(IllegalArgumentException.class, 
				() -> new AvailabilitySlot(specialist, null, end));
		
		assertEquals("Time cannot be null", exception.getMessage());
	}
	
	@Test
	public void shouldRejectCreationWhenEndTimeIsNull() {
		Specialist specialist = mock(Specialist.class);
		LocalDateTime start = now.plusHours(1);
		Exception exception = assertThrows(IllegalArgumentException.class, 
				() -> new AvailabilitySlot(specialist, start, null));
		
		assertEquals("Time cannot be null", exception.getMessage());
	}
	
	@Test
	public void shouldRejectCreationWhenStartEqualsEnd() {
		Specialist specialist = mock(Specialist.class);
		LocalDateTime start = now.plusHours(1);
    	LocalDateTime end = now.plusHours(1);
		Exception exception = assertThrows(IllegalArgumentException.class, 
				() -> new AvailabilitySlot(specialist, start, end));
		
		assertEquals("Start must be before end", exception.getMessage());
	}
	
	@Test
	public void shouldRejectCreationWhenStartIsAfterEnd() {
		Specialist specialist = mock(Specialist.class);
		LocalDateTime start = now.plusHours(2);
    	LocalDateTime end = now.plusHours(1);
		Exception exception = assertThrows(IllegalArgumentException.class, 
				() -> new AvailabilitySlot(specialist, start, end));
		
		assertEquals("Start must be before end", exception.getMessage());
	}
}
