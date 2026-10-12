package com.psyweb.booking.web.mapper;

import org.springframework.stereotype.Component;

import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.web.dto.response.ReservationResponse;

@Component
public class ReservationWebMapper {
	public ReservationResponse toResponse(Reservation reservation) {
		return new ReservationResponse(
				reservation.getId(),
				reservation.getStatus(),
				reservation.getSlotId(),
				reservation.getExpiresAt()
				);
	}
}
