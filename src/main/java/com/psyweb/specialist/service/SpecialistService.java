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

	private void validateSpecialistId(Long specialistId) {
		if (specialistId == null) {
			throw new InvalidSpecialistDataException("Specialist id cannot be null");
		}
	}

	// find specialist:

	public Specialist getSpecialist(Long specialistId) {
		validateSpecialistId(specialistId);
		Specialist specialist = specialistRepository.findById(specialistId)
				.orElseThrow(() -> new SpecialistNotFoundException("Specialist not found"));

		return specialist;
	}
	
	@Transactional
	public Specialist getSpecialistForUpdate(Long specialistId) {
		validateSpecialistId(specialistId);
		return specialistRepository.findForUpdateById(specialistId)
				.orElseThrow(() -> new SpecialistNotFoundException("Specialist not found"));
	}

	@Transactional
	public Specialist getEligibleSpecialist(Long specialistId) {
		Specialist specialist = getSpecialist(specialistId);
		if (!specialist.isEligible()) {
			throw new SpecialistNotEligibleException("Specialist is not eligible for professional operations");
		}
		return specialist;
	}

	@Transactional
	public Specialist getEligibleSpecialistForUpdate(Long specialistId) {
		Specialist specialist = getSpecialistForUpdate(specialistId);

		if (!specialist.isEligible()) {
			throw new SpecialistNotEligibleException("Specialist is not eligible for professional operations");
		}

		return specialist;
	}

	// update specialist state:

	@Transactional
	public void approveSpecialist(Long specialistId) {
		Specialist specialist = getSpecialistForUpdate(specialistId);
		specialist.approve();
		specialistRepository.save(specialist);
	}

	@Transactional
	public void rejectSpecialist(Long specialistId) {
		Specialist specialist = getSpecialistForUpdate(specialistId);
		specialist.reject();
		specialistRepository.save(specialist);
	}

	@Transactional
	public void resubmitSpecialist(Long specialistId) {
		Specialist specialist = getSpecialistForUpdate(specialistId);
		specialist.resubmit();
		specialistRepository.save(specialist);
	}

	@Transactional
	public void suspendSpecialist(Long specialistId) {
		Specialist specialist = getSpecialistForUpdate(specialistId);
		specialist.suspend();
		specialistRepository.save(specialist);
	}

	@Transactional
	public void reinstateSpecialist(Long specialistId) {
		Specialist specialist = getSpecialistForUpdate(specialistId);
		specialist.reinstate();
		specialistRepository.save(specialist);
	}
}
