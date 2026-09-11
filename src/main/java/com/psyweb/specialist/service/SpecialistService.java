package com.psyweb.specialist.service;

import org.springframework.stereotype.Service;

import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.exception.InvalidSpecialistDataException;
import com.psyweb.specialist.exception.SpecialistNotEligibleException;
import com.psyweb.specialist.exception.SpecialistNotFoundException;
import com.psyweb.specialist.repository.SpecialistRepository;

import jakarta.transaction.Transactional;

@Service
public class SpecialistService {
	private final SpecialistRepository specialistRepository;
	
	public SpecialistService(SpecialistRepository specialistRepository) {
		this.specialistRepository = specialistRepository;
	}
	
	private void validateSpecialistId(Long id) {
		if (id == null) {
			throw new InvalidSpecialistDataException("Specialist id cannot be null");
		}
	}
	
	public Specialist getSpecialist(Long id) {
		validateSpecialistId(id);
		Specialist specialist = specialistRepository.findById(id)
				.orElseThrow(() -> new SpecialistNotFoundException("Specialist not found"));
		
		return specialist;
	}
	
	public Specialist getEligibleSpecialist(Long id) {
		Specialist specialist = getSpecialist(id);
		if (!specialist.isEligible()) {
			throw new SpecialistNotEligibleException("Specialist is not eligible for professional operations");
		}
		return specialist;
	}
	
	@Transactional
	public void approveSpecialist(Long specialistId) {
		Specialist specialist = getSpecialist(specialistId);
		specialist.approve();
		specialistRepository.save(specialist);
	}
	
	@Transactional
	public void rejectSpecialist(Long specialistId) {
		Specialist specialist = getSpecialist(specialistId);
		specialist.reject();
		specialistRepository.save(specialist);
	}
	
	@Transactional
	public void resubmitSpecialist(Long specialistId) {
		Specialist specialist = getSpecialist(specialistId);
		specialist.resubmit();
		specialistRepository.save(specialist);
	}
	
	@Transactional
	public void suspendSpecialist(Long specialistId) {
		Specialist specialist = getSpecialist(specialistId);
		specialist.suspend();
		specialistRepository.save(specialist);
	}
	
	@Transactional
	public void reinstateSpecialist(Long specialistId) {
		Specialist specialist = getSpecialist(specialistId);
		specialist.reinstate();
		specialistRepository.save(specialist);
	}
}

