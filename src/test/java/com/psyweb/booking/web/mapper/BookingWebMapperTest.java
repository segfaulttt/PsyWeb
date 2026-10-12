
package com.psyweb.booking.web.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.psyweb.booking.domain.Booking;
import com.psyweb.booking.domain.BookingStatus;
import com.psyweb.booking.web.dto.response.BookingResponse;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;

class BookingWebMapperTest {

	private final BookingWebMapper mapper = new BookingWebMapper();

	private final Instant createdAt = Instant.parse("2026-11-15T10:00:00Z");

	private final Instant startTime = Instant.parse("2026-11-20T12:00:00Z");

	private final Instant endTime = Instant.parse("2026-11-20T13:00:00Z");

	@Test
	void shouldMapConfirmedBooking() {
		Booking booking = mock(Booking.class);

		when(booking.getId()).thenReturn(10L);
		when(booking.getSlotId()).thenReturn(20L);
		when(booking.getClientId()).thenReturn(30L);
		when(booking.getSpecialistId()).thenReturn(40L);
		when(booking.getStatus()).thenReturn(BookingStatus.CONFIRMED);
		when(booking.getCreatedAt()).thenReturn(createdAt);
		when(booking.getStartTime()).thenReturn(startTime);
		when(booking.getEndTime()).thenReturn(endTime);

		BookingResponse response = mapper.toResponse(booking);

		assertEquals(10L, response.id());
		assertEquals(20L, response.slotId());
		assertEquals(30L, response.clientId());
		assertEquals(40L, response.specialistId());
		assertEquals(BookingStatus.CONFIRMED, response.status());
		assertEquals(createdAt, response.createdAt());
		assertEquals(startTime, response.startTime());
		assertEquals(endTime, response.endTime());

		assertNull(response.cancelledAt());
		assertNull(response.cancellationInitiator());
		assertNull(response.cancellationReason());
	}

	@Test
	void shouldMapCancelledBooking() {
		Booking booking = mock(Booking.class);

		Instant cancelledAt = Instant.parse("2026-11-18T09:30:00Z");

		when(booking.getStatus()).thenReturn(BookingStatus.CANCELLED);
		when(booking.getCancelledAt()).thenReturn(cancelledAt);
		when(booking.getCancellationInitiator()).thenReturn(CancellationInitiator.CLIENT);
		when(booking.getCancellationReason()).thenReturn(CancellationReason.CLIENT_REQUEST);

		BookingResponse response = mapper.toResponse(booking);

		assertEquals(BookingStatus.CANCELLED, response.status());
		assertEquals(cancelledAt, response.cancelledAt());
		assertEquals(CancellationInitiator.CLIENT, response.cancellationInitiator());
		assertEquals(CancellationReason.CLIENT_REQUEST, response.cancellationReason());
	}

	@ParameterizedTest
	@EnumSource(BookingStatus.class)
	void shouldPreserveBookingStatus(BookingStatus status) {
		Booking booking = mock(Booking.class);

		when(booking.getStatus()).thenReturn(status);

		BookingResponse response = mapper.toResponse(booking);

		assertEquals(status, response.status());
	}
}