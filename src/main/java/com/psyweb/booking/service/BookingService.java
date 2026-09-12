package com.psyweb.booking.service;

import java.time.Clock;
import java.time.LocalDateTime;
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
    private final Clock clock;

	public BookingService(BookingRepository bookingRepository,  
			UserService userService, 
			SpecialistService specialistService,
			AvailabilitySlotService slotService,
			ReservationService reservationService,
			Clock clock) {
		this.bookingRepository = bookingRepository;
		this.userService = userService;
		this.specialistService = specialistService;
		this.slotService = slotService;
		this.reservationService = reservationService;
		this.clock = clock;
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
		AvailabilitySlot slot = slotService.confirmBooking(reservation.getSlotId());
		Specialist specialist = specialistService.getEligibleSpecialist(slot.getSpecialistId());
		reservation.confirm();
		LocalDateTime now = LocalDateTime.now(clock);
		Booking booking = new Booking(client, specialist, slot, reservation, now);
		
		return bookingRepository.save(booking);
	}
	
	@Transactional
	public void cancelBooking(Long bookingId) {
		if (bookingId == null) {
			throw new IllegalArgumentException("Incorrect id");
		}
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new IllegalArgumentException("Booking not found"));
		booking.cancel(LocalDateTime.now(clock));
		slotService.releaseBooking(booking.getSlotId());
	}
	
	public List<Booking> getClientBookings(Long clientId, BookingStatus status) {
		if (clientId == null) {
			throw new IllegalArgumentException("Incorrect id");
		}
		if (status == null) {
			return bookingRepository.findByClient_Id(clientId);
		}

 		return bookingRepository.findByClient_IdAndStatus(clientId, status);
	}
	
	public List<Booking> getSpecialistBookings(Long specialistId, BookingStatus status) {
		if (specialistId == null) {
			throw new IllegalArgumentException("Incorrect id");
		}
		if (status ==  null) {
			return bookingRepository.findBySpecialist_Id(specialistId);
		}
		
		return bookingRepository.findBySpecialist_IdAndStatus(specialistId, status);
	}
}