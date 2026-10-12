package com.psyweb.availability.web.mapper;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.web.dto.response.AvailableSlotResponse;
import com.psyweb.availability.web.dto.response.SpecialistAvailabilitySlotResponse;

@Component
public class AvailabilitySlotWebMapper {
	public Duration toNoticeDuration(Long minutes) {
		if (minutes == null) {
			return null;
		}
		return Duration.ofMinutes(minutes);
	}
	
	public Long toNoticeMinutes(Duration attribute) {
		if (attribute == null) {
			return null;
		}
		return attribute.toMinutes();
	}
	
	public SpecialistAvailabilitySlotResponse toSpecialistResponse(AvailabilitySlot slot) {
		return new SpecialistAvailabilitySlotResponse(
					slot.getId(),
					slot.getStartTime(),
					slot.getEndTime(),
					slot.getAvailabilityStatus(),
					toNoticeMinutes(slot.getMinimumBookingNoticeOverride()),
					toNoticeMinutes(slot.getEffectiveMinimumBookingNotice()),
					slot.getCancelledAt(),
					slot.getCancellationInitiator(),
					slot.getCancellationReason()
				);
	}
	
	public AvailableSlotResponse toAvailableResponse(AvailabilitySlot slot) {
		return new AvailableSlotResponse(
				slot.getId(),
				slot.getStartTime(),
				slot.getEndTime()
				);
	}
}
