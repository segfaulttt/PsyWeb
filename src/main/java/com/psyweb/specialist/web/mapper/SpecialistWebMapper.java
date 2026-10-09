package com.psyweb.specialist.web.mapper;

import com.psyweb.common.persistence.DurationMinutesConverter;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.web.dto.response.SpecialistBookingSettingsResponse;
import com.psyweb.specialist.web.dto.response.SpecialistProfileResponse;

public class SpecialistWebMapper {
	private final DurationMinutesConverter converter = new DurationMinutesConverter();
	public SpecialistProfileResponse toProfileResponse(Specialist specialist) {
		return new SpecialistProfileResponse(
				specialist.getId(),
				specialist.getFirstName(),
				specialist.getLastName(),
				specialist.getApprovalStatus());
	}
	
	public SpecialistBookingSettingsResponse toBookingSettingsResponse(Specialist specialist) {
		return new SpecialistBookingSettingsResponse(
				converter.convertToDatabaseColumn(specialist.getMinimumBookingNotice()),
				converter.convertToDatabaseColumn(specialist.getClientCancellationNotice()));
	}
}
