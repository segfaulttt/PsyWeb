package com.psyweb.common.persistence;

import java.time.Duration;

import jakarta.persistence.Converter;
import jakarta.persistence.AttributeConverter;

@Converter
public class DurationMinutesConverter implements AttributeConverter<Duration, Integer> {	
	@Override
	public Integer convertToDatabaseColumn(Duration attribute) {
		if (attribute == null) {
			return null;
		}
		
		if (attribute.getSeconds() % 60 != 0 || attribute.getNano() != 0) {
			throw new IllegalArgumentException("Duration must contain whole minutes");
		}
		
		long minutes = attribute.getSeconds() / 60;
		return Math.toIntExact(minutes);
	}

	@Override
	public Duration convertToEntityAttribute(Integer dbData) {
		if (dbData == null) {
			return null;
		}
		
		return Duration.ofMinutes(dbData);
	}
}
