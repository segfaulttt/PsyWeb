package com.psyweb.booking.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psyweb.booking.domain.Booking;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.repository.BookingRepository;
import com.psyweb.booking.repository.ReservationRepository;
import com.psyweb.booking.service.model.BookingObligation;
import com.psyweb.booking.service.model.ObligationType;


@Service
public class BookingObligationsQueryService {
	private final BookingRepository bookingRepository;
	private final ReservationRepository reservationRepository;

	public BookingObligationsQueryService(BookingRepository bookingRepository,
			ReservationRepository reservationRepository) {
		this.bookingRepository = bookingRepository;
		this.reservationRepository = reservationRepository;
	}

	@Transactional(readOnly = true)
	public List<BookingObligation> getFutureConfirmedBookings(Long specialistId, Instant now) {
		if (specialistId == null) {
			throw new IllegalArgumentException("Specialist ID cannot be null");
		}
		if (now == null) {
			throw new IllegalArgumentException("Time cannot be null");
		}
		List<Booking> bookings = bookingRepository.findFutureConfirmedBookingsBySpecialistId(specialistId, now);
		return bookings.stream()
				.map(b -> new BookingObligation(b.getId(), b.getStartTime(), b.getEndTime(), ObligationType.BOOKING))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<BookingObligation> getActiveFutureReservations(Long specialistId, Instant now) {
		if (specialistId == null) {
			throw new IllegalArgumentException("Specialist ID cannot be null");
		}
		if (now == null) {
			throw new IllegalArgumentException("Time cannot be null");
		}
		List<Reservation> reservations = reservationRepository.findActiveFutureReservationsBySpecialistId(specialistId,
				now);
		return reservations.stream()
				.map(r -> new BookingObligation(r.getId(), r.getStartTime(), r.getEndTime(), ObligationType.RESERVATION))
				.toList();
	}
}