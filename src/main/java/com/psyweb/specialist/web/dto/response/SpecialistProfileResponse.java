package com.psyweb.specialist.web.dto.response;

import com.psyweb.specialist.domain.SpecialistStatus;

public record SpecialistProfileResponse(
		Long id,
		String firstName,
		String lastName,
		SpecialistStatus approvalStatus) {
}