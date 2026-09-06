package com.psyweb.common.persistence;

import java.time.Duration;

import jakarta.persistence.Converter;
import jakarta.persistence.AttributeConverter;

@Converter
public class DurationMinutesConverter implements AttributeConverter<Duration, Long> {	
	@Override
	public Long convertToDatabaseColumn(Duration dbData) {
		if (dbData == null) {
			return null;
		}
		
		return dbData.toMinutes();
	}

	@Override
	public Duration convertToEntityAttribute(Long attribute) {
		if (attribute == null) {
			return null;
		}
		
		return Duration.ofMinutes(attribute);
	}
}
