package com.psyweb.admin.service;

import java.time.Clock;
import java.time.LocalDateTime;

import com.psyweb.booking.service.SchedulingService;
import com.psyweb.specialist.exception.InvalidSpecialistDataException;
import com.psyweb.specialist.service.SpecialistService;

import jakarta.transaction.Transactional;

public class SpecialistModerationService {
	private SpecialistService specialistService;
	private SchedulingService schedulingService;
	private final Clock clock;
	
	public SpecialistModerationService(SpecialistService specialistService, SchedulingService schedulingService, Clock clock) {
		this.specialistService = specialistService;
		this.schedulingService = schedulingService;
		this.clock = clock;
	}
	
	private void validateSpecialistId(Long specialistId) {
		if (specialistId == null) {
			throw new InvalidSpecialistDataException("Specialist id cannot be null");
		}
	}
	
	@Transactional
	public void approveSpecialist(Long specialistId) {
		validateSpecialistId(specialistId);
		specialistService.approveSpecialist(specialistId);
	}

	@Transactional
	public void rejectSpecialist(Long specialistId) {
		validateSpecialistId(specialistId);
		specialistService.rejectSpecialist(specialistId);
	}

	@Transactional
	public void suspendSpecialist(Long specialistId) {
		validateSpecialistId(specialistId);
		LocalDateTime suspendedAt = LocalDateTime.now(clock);
		specialistService.suspendSpecialist(specialistId);
		schedulingService.cancelFutureSlotsForSpecialistSuspension(specialistId, suspendedAt);
	}

	@Transactional
	public void reinstateSpecialist(Long specialistId) {
		validateSpecialistId(specialistId);
		specialistService.reinstateSpecialist(specialistId);
	}
}
