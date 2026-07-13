package com.psyweb.booking.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.service.AvailabilitySlotService;
import com.psyweb.booking.domain.Booking;
import com.psyweb.booking.domain.BookingStatus;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.domain.ReservationStatus;
import com.psyweb.booking.repository.BookingRepository;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.service.SpecialistService;
import com.psyweb.user.domain.User;
import com.psyweb.user.service.UserService;

import jakarta.transaction.Transactional;

@Service
public class BookingService {
	private final BookingRepository bookingRepository;
    private final UserService userService;
    private final SpecialistService specialistService;
    private final AvailabilitySlotService slotService;
    private final ReservationService reservationService;

	public BookingService(BookingRepository bookingRepository,  
			UserService userService, 
			SpecialistService specialistService,
			AvailabilitySlotService slotService,
			ReservationService reservationService) {
		this.bookingRepository = bookingRepository;
		this.userService = userService;
		this.specialistService = specialistService;
		this.slotService = slotService;
		this.reservationService = reservationService;
	}
	
	@Transactional
	public Booking confirmReservation(Long reservationId, Long clientId) {
		if (reservationId == null || clientId == null) {
			throw new IllegalArgumentException("Incorrect id");
		}
		Reservation reservation = reservationService.getReservation(reservationId);
		if (!reservation.getClientId().equals(clientId)) {
			throw new IllegalArgumentException("Reservation does not belong to this client");
		}
		if (reservation.isExpired()) {
			reservation.expire();
			throw new IllegalArgumentException("Reservation already expired");
		}
		if (reservation.getStatus() != ReservationStatus.ACTIVE) {
			throw new IllegalArgumentException("Reservation must have status 'ACTIVE'");
		}
		User client = userService.getActiveUser(clientId);
		AvailabilitySlot slot = slotService.getFreeSlot(reservation.getSlotId());
		Specialist specialist = specialistService.getActiveSpecialist(slot.getSpecialistId());
		reservation.confirm();
		Booking booking = new Booking(client, specialist, slot, reservation);
		slot.markBooked();
		
		return bookingRepository.save(booking);
	}
	
	@Transactional
	public void cancelBooking(Long bookingId) {
		if (bookingId == null) {
			throw new IllegalArgumentException("Incorrect id");
		}
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new IllegalArgumentException("Booking not found"));
		booking.cancel(LocalDateTime.now());
		slotService.releaseBookedSlot(booking.getSlotId());
	}
	
	public List<Booking> getClientBookings(Long clientId, BookingStatus status) {
		if (clientId == null) {
			throw new IllegalArgumentException("Incorrect id");
		}
		List<Booking> bookings = bookingRepository.findByClientId(clientId);
		if (status == null) {
			return bookings;
		}
		List <Booking> byStatus = new ArrayList<>();
		for (Booking b : bookings) {
			if (b.getStatus() == status) {
				byStatus.add(b);
			}
		}
 		return byStatus;
	}
	
	public List<Booking> getSpecialistBookings(Long specialistId, BookingStatus status) {
		if (specialistId == null) {
			throw new IllegalArgumentException("Incorrect id");
		}
		List<Booking> bookings = bookingRepository.findBySpecialistId(specialistId);
		if (status == null) {
			return bookings;
		}
		List <Booking> byStatus = new ArrayList<>();
		for (Booking b : bookings) {
			if (b.getStatus() == status) {
				byStatus.add(b);
			}
		}
		return byStatus;
	}
}