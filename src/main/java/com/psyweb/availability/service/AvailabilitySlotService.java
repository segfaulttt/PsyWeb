package com.psyweb.availability.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.exception.AvailabilitySlotNotFoundException;
import com.psyweb.availability.exception.InvalidAvailabilitySlotDataException;
import com.psyweb.availability.exception.InvalidAvailabilitySlotStateException;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.service.SpecialistService;

import jakarta.transaction.Transactional;

@Service
public class AvailabilitySlotService {
	private final AvailabilitySlotRepository slotRepository;
	private final SpecialistService specialistService;
	private final Clock clock;

	public AvailabilitySlotService(AvailabilitySlotRepository slotRepository, SpecialistService specialistService,
			Clock clock) {
		this.slotRepository = slotRepository;
		this.specialistService = specialistService;
		this.clock = clock;
	}
	
	public AvailabilitySlot createSlot(Long specialistId, LocalDateTime startTime, LocalDateTime endTime) {
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

		Specialist specialist = specialistService.getEligibleSpecialist(specialistId);

		if (slotRepository.existsOverlappingSlot(specialistId, startTime, endTime)) {
			throw new IllegalArgumentException("Overlap");
		}

		AvailabilitySlot newSlot = new AvailabilitySlot(specialist, startTime, endTime);

		return slotRepository.save(newSlot);
	}
	
	// Find slot:
	
	public AvailabilitySlot findSlotById(Long slotId) {
		if (slotId == null) {
			throw new InvalidAvailabilitySlotDataException("Slot id cannot be null");
		}
		return slotRepository.findById(slotId)
				.orElseThrow(() -> new AvailabilitySlotNotFoundException("Slot not found"));
	}
	
	public List<AvailabilitySlot> findBySpecialist(Long specialistId) {
		return slotRepository.findBySpecialistId(specialistId);
	}
	
	public AvailabilitySlot findSlotForUpdate(Long slotId) {
		if (slotId == null) {
			throw new InvalidAvailabilitySlotDataException("Slot id cannot be null");
		}

		return slotRepository.findForUpdateById(slotId)
				.orElseThrow(() -> new AvailabilitySlotNotFoundException("Slot not found"));
	}

	public AvailabilitySlot findFreeSlot(Long slotId) {
		if (slotId == null) {
			throw new InvalidAvailabilitySlotDataException("Invalid id");
		}
		AvailabilitySlot slot = slotRepository.findById(slotId)
				.orElseThrow(() -> new AvailabilitySlotNotFoundException("Slot not found"));
		if (slot.getAvailabilityStatus() != AvailabilityStatus.FREE) {
			throw new InvalidAvailabilitySlotStateException("Slot must have status 'FREE'");
		}
		return slot;
	}
	
	
	// Update slot state:
	
	@Transactional
	public AvailabilitySlot releaseReservation(Long slotId) {
		if (slotId == null) {
			throw new InvalidAvailabilitySlotDataException("Slot ID cannot be null");
		}

		AvailabilitySlot slot = slotRepository.findForUpdateById(slotId)
				.orElseThrow(() -> new AvailabilitySlotNotFoundException("Slot not found"));

		slot.releaseReservation();
		return slotRepository.save(slot);
	}

	@Transactional
	public AvailabilitySlot releaseBooking(Long slotId) {
		if (slotId == null) {
			throw new InvalidAvailabilitySlotDataException("Slot ID cannot be null");
		}

		AvailabilitySlot slot = slotRepository.findForUpdateById(slotId)
				.orElseThrow(() -> new AvailabilitySlotNotFoundException("Slot not found"));

		slot.releaseBooking();
		return slotRepository.save(slot);
	}
	
	@Transactional
	public AvailabilitySlot confirmBooking(Long slotId) {
		if (slotId == null) {
			throw new InvalidAvailabilitySlotDataException("Slot ID cannot be null");
		}

		AvailabilitySlot slot = slotRepository.findForUpdateById(slotId)
				.orElseThrow(() -> new AvailabilitySlotNotFoundException("Slot not found"));

		slot.confirmBooking();
		return slotRepository.save(slot);
	}

	@Transactional
	public AvailabilitySlot cancelSlot(Long slotId, LocalDateTime cancelledAt, CancellationInitiator initiator,
			CancellationReason reason) {
		if (slotId == null) {
			throw new InvalidAvailabilitySlotDataException("Slot ID cannot be null");
		}

		AvailabilitySlot slot = slotRepository.findForUpdateById(slotId)
				.orElseThrow(() -> new AvailabilitySlotNotFoundException("Slot not found"));

		slot.cancel(cancelledAt, initiator, reason);
		return slotRepository.save(slot);
	}
	
	@Transactional
	public AvailabilitySlot reserveSlot(Long slotId) {
		if (slotId == null) {
			throw new InvalidAvailabilitySlotDataException("Slot ID cannot be null");
		}

		AvailabilitySlot slot = slotRepository.findForUpdateById(slotId)
				.orElseThrow(() -> new AvailabilitySlotNotFoundException("Slot not found"));

		slot.reserve();
		return slotRepository.save(slot);
	}	
}
