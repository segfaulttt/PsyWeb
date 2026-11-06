package com.psyweb.schedule.persistence;

import java.time.DayOfWeek;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DayOfWeekConverter implements AttributeConverter<DayOfWeek, Short> {
	@Override
	public Short convertToDatabaseColumn(DayOfWeek attribute) {
		return attribute == null ? null : (short) attribute.getValue();
	}

	@Override
	public DayOfWeek convertToEntityAttribute(Short dbData) {
		if (dbData == null) {
			return null;
		}
		if (dbData < 1 || dbData > 7) {
			throw new IllegalArgumentException("Day of week must be between 1 and 7");
		}
		return DayOfWeek.of(dbData);
	}
}
