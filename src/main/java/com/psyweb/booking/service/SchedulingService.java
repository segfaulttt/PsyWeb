package com.psyweb.booking.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.exception.InvalidAvailabilitySlotDataException;
import com.psyweb.availability.exception.InvalidAvailabilitySlotStateException;
import com.psyweb.availability.service.AvailabilitySlotService;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
import com.psyweb.specialist.exception.InvalidSpecialistDataException;

import jakarta.transaction.Transactional;

@Service
public class SchedulingService {
	private final AvailabilitySlotService slotService;
	private final BookingService bookingService;
	private final ReservationService reservationService;
	private final Clock clock;

	public SchedulingService(AvailabilitySlotService slotService, BookingService bookingService,
			ReservationService reservationService, Clock clock) {
		this.bookingService = bookingService;
		this.reservationService = reservationService;
		this.slotService = slotService;
		this.clock = clock;
	}

	@Transactional
	public void removeSlotBySpecialist(Long slotId, Long specialistId) {
		if (slotId == null) {
			throw new InvalidAvailabilitySlotDataException("Slot id cannot be null");
		}
		if (specialistId == null) {
			throw new InvalidSpecialistDataException("Specialist id cannot be null");
		}
		AvailabilitySlot slot = slotService.findSlotForUpdate(slotId);
		if (!slot.getSpecialistId().equals(specialistId)) {
			throw new InvalidAvailabilitySlotStateException("Slot does not belong to this specialist");
		}
		LocalDateTime now = LocalDateTime.now(clock);
		switch (slot.getAvailabilityStatus()) {
		case FREE:
			break;
		case RESERVED:
			reservationService.cancelActiveReservationForSlotRemoval(slotId, now);
			break;
		case BOOKED:
			bookingService.cancelConfirmedBookingForSlotRemoval(slotId, now);
			break;
		case CANCELLED:
			throw new InvalidAvailabilitySlotStateException("Slot is already cancelled");
		}
		slotService.cancelSlot(slotId, now, CancellationInitiator.SPECIALIST,
				CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);
	}

}
