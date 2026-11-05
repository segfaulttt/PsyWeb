package com.psyweb.schedule.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psyweb.schedule.domain.SpecialistScheduleSettings;
import com.psyweb.schedule.domain.SpecialistTimeZoneGuard;
import com.psyweb.schedule.repository.SpecialistScheduleSettingsRepository;
import com.psyweb.schedule.repository.SpecialistTimeZoneGuardRepository;
import com.psyweb.specialist.domain.Specialist;

@Service
public class ScheduleInitializationService {

	private final SpecialistScheduleSettingsRepository settingsRepository;
	private final SpecialistTimeZoneGuardRepository guardRepository;
	private final Clock clock;

	public ScheduleInitializationService(SpecialistScheduleSettingsRepository settingsRepository,
			SpecialistTimeZoneGuardRepository guardRepository, Clock clock) {
		this.settingsRepository = settingsRepository;
		this.guardRepository = guardRepository;
		this.clock = clock;
	}

	@Transactional
	public void initialize(Specialist specialist, ZoneId timeZone) {
		if (specialist == null || timeZone == null) {
			throw new IllegalArgumentException("Specialist and time zone are required");
		}

		Instant now = Instant.now(clock);

		settingsRepository.save(new SpecialistScheduleSettings(specialist, now));

		guardRepository.save(new SpecialistTimeZoneGuard(specialist, timeZone, now));
	}
}