package com.psyweb.booking.scheduler;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.psyweb.booking.service.ReservationService;

@ExtendWith(MockitoExtension.class)
public class ReservationExpirationSchedulerTest {
	@Mock
	private ReservationService reservationService;

	private ReservationExpirationScheduler scheduler;

	@BeforeEach
	void setUp() {
		scheduler = new ReservationExpirationScheduler(reservationService);
	}

	@Test
	public void shouldDelegateExpirationToReservationService() {
		scheduler.expireReservations();

		verify(reservationService, times(1)).expireExpiredReservations();
	}
}
