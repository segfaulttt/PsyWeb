package com.psyweb.availability.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.service.SpecialistService;

@Service
public class AvailabilitySlotService {
	private final AvailabilitySlotRepository slotRepository;
	private final SpecialistService specialistService;
	private final Clock clock;

	public AvailabilitySlotService(
	        AvailabilitySlotRepository slotRepository,
	        SpecialistService specialistService,
	        Clock clock) {
	    this.slotRepository = slotRepository;
	    this.specialistService = specialistService;
	    this.clock = clock;
	}
	
	public AvailabilitySlot createSlot(Long specialistId, LocalDateTime startTime, LocalDateTime endTime) {
		if (specialistId == null) {
			throw new IllegalArgumentException("Specialist id cannot be null");
		}
		
		if (startTime == null || endTime == null) {
			throw new IllegalArgumentException("Time cannot be null");
		}
		if (!startTime.isAfter(LocalDateTime.now(clock))) {
			throw new IllegalArgumentException("Start time must be after now");
		}
		if (!startTime.isBefore(endTime)) {
			throw new IllegalArgumentException("Start must be before end");
		}
		
		Specialist specialist = specialistService.getEligibleSpecialist(specialistId);
		
		if (slotRepository.existsOverlappingSlot(specialistId, startTime, endTime)) {
			throw new IllegalArgumentException("Overlap");
		}
		
		AvailabilitySlot newSlot = new AvailabilitySlot(specialist, startTime, endTime);
		
		return slotRepository.save(newSlot);
	}
	
	public AvailabilitySlot blockSlot(Long slotId) {
		if (slotId == null) {
			throw new IllegalArgumentException("Slot ID cannot be null");
		}
		
		AvailabilitySlot slot = slotRepository.findById(slotId)
				.orElseThrow(() -> new IllegalArgumentException("Slot not found"));
		
		if (slot.getAvailabilityStatus() != AvailabilityStatus.FREE) {
			throw new IllegalArgumentException("Cannot block slot");
		}

		slot.markBlocked();
		return slotRepository.save(slot);
	}
	
	public AvailabilitySlot freeSlot(Long slotId) {
		if (slotId == null) {
			throw new IllegalArgumentException("Slot ID cannot be null");
		}
		
		AvailabilitySlot slot = slotRepository.findById(slotId)
				.orElseThrow(() -> new IllegalArgumentException("Slot not found"));
		
		if (slot.getAvailabilityStatus() != AvailabilityStatus.BLOCKED) {
			throw new IllegalArgumentException("Cannot free slot");
		}

		slot.markFree();
		return slotRepository.save(slot);
	}
	
	public AvailabilitySlot releaseBookedSlot(Long slotId) {
		if (slotId == null) {
			throw new IllegalArgumentException("Slot ID cannot be null");
		}
		
		AvailabilitySlot slot = slotRepository.findById(slotId)
				.orElseThrow(() -> new IllegalArgumentException("Slot not found"));
		
		if (slot.getAvailabilityStatus() != AvailabilityStatus.BOOKED) {
			throw new IllegalArgumentException("Cannot free slot");
		}

		slot.markFree();
		return slotRepository.save(slot);
	}
	
	public List<AvailabilitySlot> findBySpecialist(Long specialistId) {
		return slotRepository.findBySpecialistId(specialistId);
	}	
	
	public AvailabilitySlot getFreeSlot(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Invalid id");
		}
		AvailabilitySlot slot = slotRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Slot not found"));
		if (slot.getAvailabilityStatus() != AvailabilityStatus.FREE) {
			throw new IllegalArgumentException("Slot must have status 'FREE'");
		}
		return slot;
	}
}
