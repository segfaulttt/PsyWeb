package com.psyweb.booking.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.psyweb.availability.service.AvailabilitySlotService;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.domain.ReservationStatus;
import com.psyweb.booking.repository.ReservationRepository;
import com.psyweb.user.service.UserService;

import jakarta.transaction.Transactional;

@Service
public class ReservationService {
	private static final long RESERVATION_TTL_MINUTES = 1;
	private final ReservationRepository reservationRepository;
	private final UserService userService;
	private final AvailabilitySlotService slotService;
	
	public ReservationService(ReservationRepository reservationRepository, 
			UserService userService, 
			AvailabilitySlotService slotService) {
		this.reservationRepository = reservationRepository;
		this.userService = userService;
		this.slotService = slotService;
	}
	
	private Reservation loadReservation(Long reservationId) {
		return reservationRepository.findById(reservationId)
				.orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
	}
	
	@Transactional
	public Reservation createReservation(Long clientId, Long slotId) {
		if (clientId == null || slotId == null) {
			throw new IllegalArgumentException("Illegal argument");
		}
		if (reservationRepository.existsBySlotIdAndStatus(slotId, ReservationStatus.ACTIVE) ) {
			throw new IllegalArgumentException("Reservation already exists");
		}
		Reservation reservation = new Reservation(
				userService.getActiveUser(clientId),
				slotService.getFreeSlot(slotId),
				LocalDateTime.now().plusMinutes(RESERVATION_TTL_MINUTES));
			
		return reservationRepository.save(reservation);
//		later:
//		try {
//		    return reservationRepository.save(reservation);
//		} catch (DataIntegrityViolationException e) {
//		    throw new ReservationAlreadyExistsException("pupupu...");
//		}
	}

	@Transactional
	public void cancelReservation(Long reservationId) {
		Reservation reservation = loadReservation(reservationId);
		reservation.cancel();
	}
	
	@Transactional
	public void expireReservation(Long reservationId) {
		if (reservationId == null) {
			throw new IllegalArgumentException("Incorrect id");
		}
		Reservation reservation = loadReservation(reservationId);
		reservation.expire();
	}
	
	@Transactional
	public void expireExpiredReservations() {
		List<Reservation> reservations = reservationRepository
				.findByStatus(ReservationStatus.ACTIVE);
		
		for (Reservation r : reservations) {
			if (r.isExpired()) {
				r.expire();
			}
		}
	}
	
	public Reservation getActiveReservationById(Long reservationId) {
		Reservation reservation = getReservation(reservationId);
		if (reservation.getStatus() != ReservationStatus.ACTIVE) {
			throw new IllegalArgumentException("Reservation must have status 'ACTIVE'");
		}
		return reservation;
	}
	
	public Reservation getReservation(Long reservationId) {
		if (reservationId == null) {
			throw new IllegalArgumentException("Incorrect Id");
		}
		Reservation reservation = loadReservation(reservationId);
		return reservation;
	}
}
