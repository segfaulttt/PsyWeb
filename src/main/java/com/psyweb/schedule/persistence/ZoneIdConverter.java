package com.psyweb.schedule.persistence;

import java.time.ZoneId;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ZoneIdConverter implements AttributeConverter<ZoneId, String>{
	@Override
	public String convertToDatabaseColumn(ZoneId attribute) {
		return attribute == null ? null :attribute.getId();
	}

	@Override
	public ZoneId convertToEntityAttribute(String dbData) {
		return dbData == null ? null : ZoneId.of(dbData);
	}
}
