package com.psyweb.booking.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.psyweb.booking.service.ReservationService;

@Component
public class ReservationExpirationScheduler {
	private final ReservationService reservationService;

	public ReservationExpirationScheduler(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@Scheduled(fixedDelayString = "${psyweb.reservation.expiration-scan-interval}")
	public void expireReservations() {
		reservationService.expireExpiredReservations();
	}
}
