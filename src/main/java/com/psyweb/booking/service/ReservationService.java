package com.psyweb.booking.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.service.AvailabilitySlotService;
import com.psyweb.booking.config.ReservationProperties;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.domain.ReservationStatus;
import com.psyweb.booking.exception.ActiveReservationAlreadyExistsException;
import com.psyweb.booking.exception.InvalidReservationDataException;
import com.psyweb.booking.exception.InvalidReservationStateException;
import com.psyweb.booking.exception.ReservationNotFoundException;
import com.psyweb.booking.exception.ReservationOwnershipException;
import com.psyweb.booking.repository.ReservationRepository;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.exception.InvalidUserStateException;
import com.psyweb.user.service.UserService;

import jakarta.transaction.Transactional;

@Service
public class ReservationService {
	private final ReservationRepository reservationRepository;
	private final UserService userService;
	private final AvailabilitySlotService slotService;
	private final Clock clock;
	private final ReservationProperties reservationProperties;

	public ReservationService(ReservationRepository reservationRepository, UserService userService,
			AvailabilitySlotService slotService, Clock clock, ReservationProperties reservationProperties) {
		this.reservationRepository = reservationRepository;
		this.userService = userService;
		this.slotService = slotService;
		this.clock = clock;
		this.reservationProperties = reservationProperties;
	}

	private Reservation loadReservation(Long reservationId) {
		return reservationRepository.findById(reservationId)
				.orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
	}

	private Reservation loadReservationForUpdate(Long reservationId) {
		return reservationRepository.findForUpdateById(reservationId)
				.orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
	}

	@Transactional
	public Reservation createReservation(Long clientId, Long slotId) {
		if (clientId == null) {
			throw new InvalidReservationDataException("Client id cannot be null");
		}
		if (slotId == null) {
			throw new InvalidReservationDataException("Slot id cannot be null");
		}
		if (reservationRepository.existsBySlotIdAndStatus(slotId, ReservationStatus.ACTIVE)) {
			throw new ActiveReservationAlreadyExistsException("Slot is already reserved");
		}
		User user = userService.getActiveUser(clientId);
		if (!user.getRole().equals(UserRole.CLIENT)) {
			throw new InvalidUserStateException("Client must have role 'CLIENT'");
		}

		try {
			AvailabilitySlot slot = slotService.reserveSlot(slotId);
			LocalDateTime now = LocalDateTime.now(clock);
			Reservation reservation = new Reservation(user, slot, now, now.plus(reservationProperties.ttl()));
			return reservationRepository.saveAndFlush(reservation);
		} catch (DataIntegrityViolationException e) {
			if (isActiveReservationConstraintViolation(e)) {
				throw new ActiveReservationAlreadyExistsException("Slot is already reserved", e);
			}
			throw e;
		}
	}

	private boolean isActiveReservationConstraintViolation(DataIntegrityViolationException exception) {
		Throwable cause = exception;

		while (cause != null) {
			if (cause instanceof ConstraintViolationException constraintException) {
				return "unique_active_reservation_slot".equals(constraintException.getConstraintName());
			}
			cause = cause.getCause();
		}

		return false;
	}

	@Transactional
	public void cancelReservationByClient(Long reservationId, Long clientId, LocalDateTime cancelledAt,
			CancellationReason reason) {
		if (reservationId == null) {
			throw new InvalidReservationDataException("Reservation id cannot be null");
		}

		if (clientId == null) {
			throw new InvalidReservationDataException("Client id cannot be null");
		}
		Reservation reservation = loadReservationForUpdate(reservationId);
		if (!reservation.getClientId().equals(clientId)) {
			throw new ReservationOwnershipException("Reservation does not belong to this client");
		}
		cancelReservation(reservation, cancelledAt, CancellationInitiator.CLIENT, reason);
	}

	private void cancelReservation(Reservation reservation, LocalDateTime cancelledAt, CancellationInitiator initiator,
			CancellationReason reason) {
		if (reservation == null) {
			throw new InvalidReservationDataException("Reservation cannot be null");
		}
		reservation.cancel(cancelledAt, initiator, reason);
		slotService.releaseReservation(reservation.getSlotId());
	}

	@Transactional
	public void expireReservation(Long reservationId) {
		if (reservationId == null) {
			throw new InvalidReservationDataException("Reservation id cannot be null");
		}
		Reservation reservation = loadReservationForUpdate(reservationId);
		LocalDateTime now = LocalDateTime.now(clock);
		reservation.expire(now);
		slotService.releaseReservation(reservation.getSlotId());

	}

	@Transactional
	public void expireExpiredReservations() {
		LocalDateTime now = LocalDateTime.now(clock);
		List<Reservation> reservations = reservationRepository.findExpiredBatchForUpdateSkipLocked(now,
				reservationProperties.expirationBatchSize());

		for (Reservation reservation : reservations) {
			if (reservation.isExpired(now)) {
				reservation.expire(now);
				slotService.releaseReservation(reservation.getSlotId());
			}
		}
	}

	public Reservation getActiveReservationById(Long reservationId) {
		Reservation reservation = getReservation(reservationId);
		if (reservation.getStatus() != ReservationStatus.ACTIVE) {
			throw new InvalidReservationStateException("Reservation must have status 'ACTIVE'");
		}
		return reservation;
	}

	public Reservation getReservation(Long reservationId) {
		if (reservationId == null) {
			throw new InvalidReservationDataException("Reservation id cannot be null");
		}
		Reservation reservation = loadReservation(reservationId);
		return reservation;
	}

	public Reservation getReservationForUpdate(Long reservationId) {
		if (reservationId == null) {
			throw new InvalidReservationDataException("Reservation id cannot be null");
		}
		Reservation reservation = loadReservationForUpdate(reservationId);
		return reservation;
	}

	public void cancelActiveReservationForSlotRemoval(Long slotId, LocalDateTime cancelledAt) {
		if (slotId == null) {
			throw new InvalidReservationDataException("Slot id cannot be null");
		}

		Reservation reservation = reservationRepository.findActiveForUpdateBySlotId(slotId)
				.orElseThrow(() -> new ReservationNotFoundException("Active reservation for slot not found"));

		cancelReservation(reservation, cancelledAt, CancellationInitiator.SPECIALIST,
				CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);
	}

	@Transactional
	public void cancelActiveReservationForSpecialistSuspension(Long slotId, LocalDateTime suspendedAt) {
		if (slotId == null) {
			throw new InvalidReservationDataException("Slot id cannot be null");
		}
		Reservation reservation = reservationRepository.findActiveForUpdateBySlotId(slotId)
				.orElseThrow(() -> new ReservationNotFoundException("Active reservation not found"));
		reservation.cancel(suspendedAt, CancellationInitiator.ADMIN, CancellationReason.SPECIALIST_SUSPENDED);
	}
}
