package com.psyweb.common.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

public class DurationMinutesConverterTest {
	private final DurationMinutesConverter converter = new DurationMinutesConverter();
	
	@Test
	public void shouldReturnLongFromDuration() {
		Duration duration = Duration.ofMinutes(65);
		
		Long result = converter.convertToDatabaseColumn(duration);
		
		assertEquals(65, result.longValue());
	}
	
	@Test
	public void shouldReturnNullInBothDirectionsWhenInputIsNull() {		
		assertNull(converter.convertToDatabaseColumn(null));
		assertNull(converter.convertToEntityAttribute(null));
	}
	
	@Test
	public void shouldReturnZeroLongFromZeroDuration() {
		Duration duration = Duration.ZERO;
		
		Long result = converter.convertToDatabaseColumn(duration);
		
		assertEquals(0L, result.longValue());
	}
	
	@Test
	public void shouldDropSecondsWhenDurationHasFractionalMinutes() {
		Duration duration = Duration.ofMinutes(5).plusSeconds(5);
		
		Long result = converter.convertToDatabaseColumn(duration);
		
		assertEquals(5L, result.longValue());
	}
	
	@Test
	public void shouldReturnDurationFromLong() {
		Long duration = Long.valueOf(33);
		
		Duration result = converter.convertToEntityAttribute(duration);
		
		assertEquals(33, result.toMinutes());
	}
	
	@Test
	public void shouldReturnZeroDurationFromZeroLong() {
		Long duration = 0L;
		
		Duration result = converter.convertToEntityAttribute(duration);
		
		assertEquals(0L, result.toMinutes());
	}
	
	@Test
	public void shouldPreserveValueWhenConvertingRoundTrip() {
		Duration originalDuration = Duration.ofMinutes(90);
        
        Long dbValue = converter.convertToDatabaseColumn(originalDuration);
        Duration restoredDuration = converter.convertToEntityAttribute(dbValue);
        
        assertEquals(originalDuration, restoredDuration);
	}
	
	@Test
    void shouldRestoreDurationFromDatabaseColumnValue() {
        Long dbValue = 60L;
        
        Duration result = converter.convertToEntityAttribute(dbValue);
        
        assertEquals(Duration.ofHours(1), result);
    }
	
	@Test
	public void shouldNotBlockTechnicallyRepresentableNegativeDuration() {
		Duration negativeDuration = Duration.ofMinutes(-15);
		
		Long result = converter.convertToDatabaseColumn(negativeDuration);
		
		assertEquals(-15L, result);
		assertEquals(negativeDuration, converter.convertToEntityAttribute(result));
	}
}
