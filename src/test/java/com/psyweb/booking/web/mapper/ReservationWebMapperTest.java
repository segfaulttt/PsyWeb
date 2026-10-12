
package com.psyweb.booking.web.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.domain.ReservationStatus;
import com.psyweb.booking.web.dto.response.ReservationResponse;

class ReservationWebMapperTest {

	private final ReservationWebMapper mapper = new ReservationWebMapper();

	private final Instant expiresAt = Instant.parse("2026-11-15T12:05:00Z");

	@Test
	void shouldMapAllReservationFields() {
		Reservation reservation = mock(Reservation.class);

		when(reservation.getId()).thenReturn(10L);
		when(reservation.getSlotId()).thenReturn(25L);
		when(reservation.getStatus()).thenReturn(ReservationStatus.ACTIVE);
		when(reservation.getExpiresAt()).thenReturn(expiresAt);

		ReservationResponse response = mapper.toResponse(reservation);

		assertEquals(10L, response.id());
		assertEquals(25L, response.slotId());
		assertEquals(ReservationStatus.ACTIVE, response.status());
		assertEquals(expiresAt, response.expiresAt());
	}

	@ParameterizedTest
	@EnumSource(ReservationStatus.class)
	void shouldPreserveReservationStatus(ReservationStatus status) {
		Reservation reservation = mock(Reservation.class);

		when(reservation.getStatus()).thenReturn(status);

		ReservationResponse response = mapper.toResponse(reservation);

		assertEquals(status, response.status());
	}

	@Test
	void shouldPreserveExpirationTimestamp() {
		Reservation reservation = mock(Reservation.class);

		Instant expiration = Instant.parse("2026-12-31T23:59:59Z");

		when(reservation.getExpiresAt()).thenReturn(expiration);

		ReservationResponse response = mapper.toResponse(reservation);

		assertEquals(expiration, response.expiresAt());
	}
}
