package com.psyweb.specialist.web.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.domain.SpecialistStatus;
import com.psyweb.specialist.web.dto.response.SpecialistBookingSettingsResponse;
import com.psyweb.specialist.web.dto.response.SpecialistProfileResponse;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

class SpecialistWebMapperTest {

	private final SpecialistWebMapper mapper = new SpecialistWebMapper();

	@Test
	void shouldMapSpecialistToProfileResponse() {
		User user = new User("specialist@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE);

		Specialist specialist = new Specialist(user, "Anna", "Smith", Duration.ofMinutes(60), Duration.ofMinutes(120));

		SpecialistProfileResponse response = mapper.toProfileResponse(specialist);

		assertEquals(specialist.getId(), response.id());
		assertEquals("Anna", response.firstName());
		assertEquals("Smith", response.lastName());
		assertEquals(SpecialistStatus.PENDING, response.approvalStatus());
	}

	@Test
	void shouldMapSpecialistToBookingSettingsResponse() {
		User user = new User("specialist@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE);

		Specialist specialist = new Specialist(user, "Anna", "Smith", Duration.ofMinutes(60), Duration.ofMinutes(120));

		SpecialistBookingSettingsResponse response = mapper.toBookingSettingsResponse(specialist);

		assertEquals(60L, response.minimumBookingNoticeMinutes());
		assertEquals(120L, response.clientCancellationNoticeMinutes());
	}
}