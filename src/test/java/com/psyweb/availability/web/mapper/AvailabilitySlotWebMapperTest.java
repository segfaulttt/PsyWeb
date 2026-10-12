
package com.psyweb.availability.web.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.web.dto.response.AvailableSlotResponse;
import com.psyweb.availability.web.dto.response.SpecialistAvailabilitySlotResponse;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

class AvailabilitySlotWebMapperTest {

	private final AvailabilitySlotWebMapper mapper = new AvailabilitySlotWebMapper();

	private final Instant startTime = Instant.parse("2026-11-15T10:00:00Z");

	private final Instant endTime = Instant.parse("2026-11-15T11:00:00Z");

	private Specialist createSpecialist() {
		User user = new User("specialist@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE);

		return new Specialist(user, "Anna", "Smith", Duration.ofMinutes(60), Duration.ofMinutes(120));
	}

	@Test
	void shouldConvertMinutesToDuration() {
		assertEquals(Duration.ofMinutes(30), mapper.toNoticeDuration(30L));
	}

	@Test
	void shouldConvertZeroMinutesToZeroDuration() {
		assertEquals(Duration.ZERO, mapper.toNoticeDuration(0L));
	}

	@Test
	void shouldPreserveNullWhenConvertingMinutesToDuration() {
		assertNull(mapper.toNoticeDuration(null));
	}

	@Test
	void shouldConvertDurationToMinutes() {
		assertEquals(90L, mapper.toNoticeMinutes(Duration.ofMinutes(90)));
	}

	@Test
	void shouldConvertZeroDurationToZeroMinutes() {
		assertEquals(0L, mapper.toNoticeMinutes(Duration.ZERO));
	}

	@Test
	void shouldPreserveNullWhenConvertingDurationToMinutes() {
		assertNull(mapper.toNoticeMinutes(null));
	}

	@Test
	void shouldMapSlotWithInheritedMinimumBookingNotice() {
		Specialist specialist = createSpecialist();

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, endTime);

		SpecialistAvailabilitySlotResponse response = mapper.toSpecialistResponse(slot);

		assertEquals(slot.getId(), response.id());
		assertEquals(startTime, response.startTime());
		assertEquals(endTime, response.endTime());
		assertEquals(AvailabilityStatus.FREE, response.status());

		assertNull(response.minimumBookingNoticeOverrideMinutes());
		assertEquals(60L, response.effectiveMinimumBookingNoticeMinutes());

		assertNull(response.cancelledAt());
		assertNull(response.cancellationInitiator());
		assertNull(response.cancellationReason());
	}

	@Test
	void shouldMapSlotWithCustomMinimumBookingNotice() {
		Specialist specialist = createSpecialist();

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, endTime, Duration.ofMinutes(30));

		SpecialistAvailabilitySlotResponse response = mapper.toSpecialistResponse(slot);

		assertEquals(30L, response.minimumBookingNoticeOverrideMinutes());
		assertEquals(30L, response.effectiveMinimumBookingNoticeMinutes());
	}

	@Test
	void shouldPreserveExplicitZeroOverride() {
		Specialist specialist = createSpecialist();

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, endTime, Duration.ZERO);

		SpecialistAvailabilitySlotResponse response = mapper.toSpecialistResponse(slot);

		assertEquals(0L, response.minimumBookingNoticeOverrideMinutes());
		assertEquals(0L, response.effectiveMinimumBookingNoticeMinutes());
	}

	@Test
	void shouldMapCancelledSlotMetadata() {
		Specialist specialist = createSpecialist();

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, endTime);

		Instant cancelledAt = Instant.parse("2026-11-14T12:00:00Z");

		slot.cancel(cancelledAt, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);

		SpecialistAvailabilitySlotResponse response = mapper.toSpecialistResponse(slot);

		assertEquals(AvailabilityStatus.CANCELLED, response.status());
		assertEquals(cancelledAt, response.cancelledAt());

		assertEquals(CancellationInitiator.SPECIALIST, response.cancellationInitiator());

		assertEquals(CancellationReason.SPECIALIST_REMOVED_AVAILABILITY, response.cancellationReason());
	}

	@Test
	void shouldMapSlotToAvailableResponse() {
		Specialist specialist = createSpecialist();

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, endTime);

		AvailableSlotResponse response = mapper.toAvailableResponse(slot);

		assertEquals(slot.getId(), response.id());
		assertEquals(startTime, response.startTime());
		assertEquals(endTime, response.endTime());
	}
}