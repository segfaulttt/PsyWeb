package com.psyweb.common.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

public class DurationMinutesConverterTest {
	private final DurationMinutesConverter converter = new DurationMinutesConverter();
	
	@Test
	public void shouldReturnIntegerFromDuration() {
		Duration duration = Duration.ofMinutes(65);
		
		Integer result = converter.convertToDatabaseColumn(duration);
		
		assertEquals(65, result);
	}
	
	@Test
	public void shouldReturnNullInBothDirectionsWhenInputIsNull() {		
		assertNull(converter.convertToDatabaseColumn(null));
		assertNull(converter.convertToEntityAttribute(null));
	}
	
	@Test
	public void shouldReturnZeroIntegerFromZeroDuration() {
		Duration duration = Duration.ZERO;
		
		Integer result = converter.convertToDatabaseColumn(duration);
		
		assertEquals(0, result);
	}
	
	@Test
	public void shouldRejectDurationWithFractionalMinutes() {
		Duration duration = Duration.ofMinutes(5).plusSeconds(5);
		
		Exception exception = assertThrows(IllegalArgumentException.class, 
				() -> converter.convertToDatabaseColumn(duration));
		
		assertEquals("Duration must contain whole minutes", exception.getMessage());
	}
	
	@Test
	public void shouldReturnDurationFromInteger() {
		Integer duration = 33;
		
		Duration result = converter.convertToEntityAttribute(duration);
		
		assertEquals(Duration.ofMinutes(33), result);
	}
	
	@Test
	public void shouldReturnZeroDurationFromZeroInteger() {
		Integer duration = 0;
		
		Duration result = converter.convertToEntityAttribute(duration);
		
		assertEquals(0L, result.toMinutes());
	}
	
	@Test
	public void shouldPreserveValueWhenConvertingRoundTrip() {
		Duration originalDuration = Duration.ofMinutes(90);
        
		Integer dbValue = converter.convertToDatabaseColumn(originalDuration);
        Duration restoredDuration = converter.convertToEntityAttribute(dbValue);
        
        assertEquals(originalDuration, restoredDuration);
	}
	
	@Test
    void shouldRestoreDurationFromDatabaseColumnValue() {
		Integer dbValue = 60;
        
        Duration result = converter.convertToEntityAttribute(dbValue);
        
        assertEquals(Duration.ofHours(1), result);
    }
	
	@Test
	public void shouldNotBlockTechnicallyRepresentableNegativeDuration() {
		Duration negativeDuration = Duration.ofMinutes(-15);
		
		Integer result = converter.convertToDatabaseColumn(negativeDuration);
		
		assertEquals(-15, result);
		assertEquals(negativeDuration, converter.convertToEntityAttribute(result));
	}
	
	@Test
	void shouldRejectDurationExceedingIntegerRange() {
	    Duration duration = Duration.ofMinutes((long) Integer.MAX_VALUE + 1);

	    assertThrows(ArithmeticException.class,
	    		() -> converter.convertToDatabaseColumn(duration));
	}
	
	@Test
	void shouldRejectDurationContainingNanoseconds() {
	    Duration duration = Duration.ofMinutes(5).plusNanos(1);

	    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
	    		() -> converter.convertToDatabaseColumn(duration));

	    assertEquals("Duration must contain whole minutes", exception.getMessage());
	}
}
