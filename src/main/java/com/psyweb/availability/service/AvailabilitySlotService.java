package com.psyweb.availability.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.specialist.domain.Specialist;

@Service
public class AvailabilitySlotService {
	private final AvailabilitySlotRepository slotRepository;
	
	public AvailabilitySlotService(AvailabilitySlotRepository slotRepository) {
		this.slotRepository = slotRepository;
	}
	
	public AvailabilitySlot createSlot(Specialist specialist, LocalDateTime startTime, LocalDateTime endTime) {
		if (specialist == null) {
			throw new IllegalArgumentException("specialist cannot be null");
		}
		if (startTime == null || endTime == null) {
			throw new IllegalArgumentException("Time cannot be null");
		}
		if (!startTime.isBefore(endTime)) {
			throw new IllegalArgumentException("Start must be before end");
		}
		if (slotRepository.existsOverlappingSlot(specialist.getId(), startTime, endTime)) {
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
	
	public List<AvailabilitySlot> findBySpecialist(Long specialistId) {
		return slotRepository.findBySpecialistId(specialistId);
	}	
}
