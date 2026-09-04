package com.psyweb.specialist.service;

import org.springframework.stereotype.Service;

import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.domain.SpecialistStatus;
import com.psyweb.specialist.exception.InvalidSpecialistDataException;
import com.psyweb.specialist.exception.SpecialistNotApprovedException;
import com.psyweb.specialist.exception.SpecialistNotFoundException;
import com.psyweb.specialist.repository.SpecialistRepository;

@Service
public class SpecialistService {
	private final SpecialistRepository specialistRepository;
	
	public SpecialistService(SpecialistRepository specialistRepository) {
		this.specialistRepository = specialistRepository;
	}
	
	public Specialist getSpecialist(Long id) {
		if (id == null) {
			throw new InvalidSpecialistDataException("Specialist id cannot be null");
		}
		Specialist specialist = specialistRepository.findById(id)
				.orElseThrow(() -> new SpecialistNotFoundException("Specialist not found"));
		
		return specialist;
	}
	
	public Specialist getActiveSpecialist(Long id) {
		Specialist specialist = getSpecialist(id);
		if (specialist.getApprovalStatus() != SpecialistStatus.APPROVED) {
			throw new SpecialistNotApprovedException("Specialist must have status 'APPROVED'");
		}
		return specialist;
	}
}

