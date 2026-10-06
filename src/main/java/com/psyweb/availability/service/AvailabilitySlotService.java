package com.psyweb.availability.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.hibernate.exception.ConstraintViolationException;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.exception.AvailabilitySlotNotFoundException;
import com.psyweb.availability.exception.InvalidAvailabilitySlotDataException;
import com.psyweb.availability.exception.InvalidAvailabilitySlotStateException;
import com.psyweb.availability.exception.SlotOverlapException;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.service.SpecialistService;

import org.postgresql.util.PSQLException;

import jakarta.transaction.Transactional;

@Service
public class AvailabilitySlotService {
	private static final String SLOT_OVERLAP_CONSTRAINT = "no_overlapping_active_slots";

	private final AvailabilitySlotRepository slotRepository;
	private final SpecialistService specialistService;
	private final Clock clock;

	public AvailabilitySlotService(AvailabilitySlotRepository slotRepository, SpecialistService specialistService,
			Clock clock) {
		this.slotRepository = slotRepository;
		this.specialistService = specialistService;
		this.clock = clock;
	}

	private void validateSlotId(Long slotId) {
		if (slotId == null) {
			throw new InvalidAvailabilitySlotDataException("Slot id cannot be null");
		}
	}

	@Transactional
	public AvailabilitySlot createSlot(Long specialistId, LocalDateTime startTime, LocalDateTime endTime,
			Duration minimumBookingNoticeOverride) {
		if (specialistId == null) {
			throw new InvalidAvailabilitySlotDataException("Specialist id cannot be null");
		}

		if (startTime == null || endTime == null) {
			throw new InvalidAvailabilitySlotDataException("Time cannot be null");
		}
		if (!startTime.isAfter(LocalDateTime.now(clock))) {
			throw new InvalidAvailabilitySlotDataException("Start time must be after now");
		}
		if (!startTime.isBefore(endTime)) {
			throw new InvalidAvailabilitySlotDataException("Start must be before end");
		}

		Specialist specialist = specialistService.getEligibleSpecialistForUpdate(specialistId);

		if (slotRepository.existsOverlappingSlot(specialistId, startTime, endTime)) {
			throw new SlotOverlapException("Slot overlap");
		}

		try {
			AvailabilitySlot newSlot = new AvailabilitySlot(specialist, startTime, endTime, minimumBookingNoticeOverride);
			return slotRepository.saveAndFlush(newSlot);
		} catch (DataIntegrityViolationException e) {
			if (isSlotOverlapConstraintViolation(e)) {
				throw new SlotOverlapException("Slot overlap", e);
			}
			throw e;
		}
	}
	
	@Transactional
	public AvailabilitySlot createSlot(Long specialistId, LocalDateTime startTime, LocalDateTime endTime) {
		return createSlot(specialistId, startTime, endTime, null);
	}

	private boolean isSlotOverlapConstraintViolation(DataIntegrityViolationException exception) {

		Throwable cause = exception;

		while (cause != null) {
			if (cause instanceof ConstraintViolationException constraintException
					&& SLOT_OVERLAP_CONSTRAINT.equals(constraintException.getConstraintName())) {
				return true;
			}

			if (cause instanceof PSQLException postgresException && postgresException.getServerErrorMessage() != null
					&& SLOT_OVERLAP_CONSTRAINT.equals(postgresException.getServerErrorMessage().getConstraint())) {
				return true;
			}

			cause = cause.getCause();
		}

		return false;
	}

	// Find slot:

	public AvailabilitySlot findSlotById(Long slotId) {
		validateSlotId(slotId);
		return slotRepository.findById(slotId)
				.orElseThrow(() -> new AvailabilitySlotNotFoundException("Slot not found"));
	}

	public List<AvailabilitySlot> findBySpecialist(Long specialistId) {
		return slotRepository.findBySpecialistId(specialistId);
	}

	public AvailabilitySlot findSlotForUpdate(Long slotId) {
		validateSlotId(slotId);
		return slotRepository.findForUpdateById(slotId)
				.orElseThrow(() -> new AvailabilitySlotNotFoundException("Slot not found"));
	}

	public AvailabilitySlot findFreeSlot(Long slotId) {
		validateSlotId(slotId);
		AvailabilitySlot slot = slotRepository.findById(slotId)
				.orElseThrow(() -> new AvailabilitySlotNotFoundException("Slot not found"));
		if (slot.getAvailabilityStatus() != AvailabilityStatus.FREE) {
			throw new InvalidAvailabilitySlotStateException("Slot must have status 'FREE'");
		}
		return slot;
	}

	@Transactional
	public List<AvailabilitySlot> findFutureSlotsForUpdate(Long specialistId, LocalDateTime suspendedAt) {
		if (specialistId == null) {
			throw new InvalidAvailabilitySlotDataException("Specialist id cannot be null");
		}

		if (suspendedAt == null) {
			throw new InvalidAvailabilitySlotDataException("Suspension time cannot be null");
		}

		return slotRepository.findFutureForUpdateBySpecialistId(specialistId, suspendedAt);
	}

	// Update slot state:

	@Transactional
	public AvailabilitySlot releaseReservation(Long slotId) {
		validateSlotId(slotId);
		AvailabilitySlot slot = slotRepository.findForUpdateById(slotId)
				.orElseThrow(() -> new AvailabilitySlotNotFoundException("Slot not found"));

		slot.releaseReservation();
		return slotRepository.save(slot);
	}

	@Transactional
	public AvailabilitySlot releaseBooking(Long slotId) {
		validateSlotId(slotId);
		AvailabilitySlot slot = slotRepository.findForUpdateById(slotId)
				.orElseThrow(() -> new AvailabilitySlotNotFoundException("Slot not found"));

		slot.releaseBooking();
		return slotRepository.save(slot);
	}

	@Transactional
	public AvailabilitySlot confirmBooking(Long slotId) {
		validateSlotId(slotId);
		AvailabilitySlot slot = slotRepository.findForUpdateById(slotId)
				.orElseThrow(() -> new AvailabilitySlotNotFoundException("Slot not found"));

		slot.confirmBooking();
		return slotRepository.save(slot);
	}

	@Transactional
	public AvailabilitySlot cancelSlot(Long slotId, LocalDateTime cancelledAt, CancellationInitiator initiator,
			CancellationReason reason) {
		validateSlotId(slotId);
		AvailabilitySlot slot = slotRepository.findForUpdateById(slotId)
				.orElseThrow(() -> new AvailabilitySlotNotFoundException("Slot not found"));

		slot.cancel(cancelledAt, initiator, reason);
		return slotRepository.save(slot);
	}

	@Transactional
	public AvailabilitySlot reserveSlot(Long slotId) {
		validateSlotId(slotId);
		AvailabilitySlot slot = slotRepository.findForUpdateById(slotId)
				.orElseThrow(() -> new AvailabilitySlotNotFoundException("Slot not found"));

		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime bookingDeadline = slot.getBookingDeadline();

		if (!now.isBefore(bookingDeadline)) {
			throw new InvalidAvailabilitySlotStateException("Slot is no longer bookable");
		}
		slot.reserve();
		return slotRepository.save(slot);
	}
}
