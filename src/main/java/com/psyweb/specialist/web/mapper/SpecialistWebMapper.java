package com.psyweb.specialist.web.mapper;

import org.springframework.stereotype.Component;

import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.web.dto.response.SpecialistBookingSettingsResponse;
import com.psyweb.specialist.web.dto.response.SpecialistProfileResponse;

@Component
public class SpecialistWebMapper {
	public SpecialistProfileResponse toProfileResponse(Specialist specialist) {
		return new SpecialistProfileResponse(
				specialist.getId(),
				specialist.getFirstName(),
				specialist.getLastName(),
				specialist.getApprovalStatus());
	}
	
	public SpecialistBookingSettingsResponse toBookingSettingsResponse(Specialist specialist) {
		return new SpecialistBookingSettingsResponse(
				specialist.getMinimumBookingNotice().toMinutes(),
				specialist.getClientCancellationNotice().toMinutes());
	}
}
