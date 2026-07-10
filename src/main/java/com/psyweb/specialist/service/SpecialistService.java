package com.psyweb.specialist.service;

import org.springframework.stereotype.Service;

import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.domain.SpecialistStatus;
import com.psyweb.specialist.repository.SpecialistRepository;

@Service
public class SpecialistService {
	private final SpecialistRepository specialistRepository;
	
	public SpecialistService(SpecialistRepository specialistRepository) {
		this.specialistRepository = specialistRepository;
	}
	
	public Specialist getActiveSpecialist(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Invalid id");
		}
		Specialist specialist = specialistRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Specialist not found"));
		if (specialist.getApprovalStatus() != SpecialistStatus.APPROVED) {
			throw new IllegalArgumentException("Specialist must have status 'APPROVED'");
		}
		return specialist;
	}
}

