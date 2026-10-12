package com.psyweb.booking.web.mapper;

import org.springframework.stereotype.Component;

import com.psyweb.booking.domain.Booking;
import com.psyweb.booking.web.dto.response.BookingResponse;

@Component
public class BookingWebMapper {
	public BookingResponse toResponse(Booking booking) {
		return new BookingResponse(
				booking.getId(),
				booking.getSlotId(),
				booking.getClientId(),
				booking.getSpecialistId(),
				booking.getStatus(),
				booking.getCreatedAt(),
				booking.getCancelledAt(),
				booking.getStartTime(),
				booking.getEndTime(),
				booking.getCancellationInitiator(),
				booking.getCancellationReason()
				);
	}
}
