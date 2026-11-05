package com.psyweb.schedule.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.psyweb.schedule.domain.DateSchedule;
import com.psyweb.schedule.domain.DateScheduleInterval;
import com.psyweb.schedule.domain.DateSchedulePurpose;
import com.psyweb.schedule.domain.ScheduleMode;
import com.psyweb.schedule.domain.SpecialistScheduleSettings;
import com.psyweb.schedule.domain.WeeklyScheduleVersion;
import com.psyweb.schedule.domain.WeeklyWorkingInterval;
import com.psyweb.schedule.repository.DateScheduleRepository;
import com.psyweb.schedule.repository.SpecialistScheduleSettingsRepository;
import com.psyweb.schedule.repository.WeeklyScheduleVersionRepository;
import com.psyweb.schedule.service.model.CreateWeeklyScheduleVersionCommand;
import com.psyweb.schedule.service.model.WeeklyWorkingIntervalCommand;
import com.psyweb.schedule.service.model.WorkingInterval;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.service.SpecialistService;

import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleManagementService {
	private final SpecialistScheduleSettingsRepository specialistScheduleSettingsRepository;
	private final WeeklyScheduleVersionRepository weeklyScheduleVersionRepository;
	private final DateScheduleRepository dateScheduleRepository;
	private final SpecialistService specialistService;
	private final Clock clock;

	public ScheduleManagementService(SpecialistScheduleSettingsRepository specialistScheduleSettingsRepository,
			WeeklyScheduleVersionRepository weeklyScheduleVersionRepository,
			DateScheduleRepository dateScheduleRepository, Clock clock, SpecialistService specialistService) {
		this.specialistScheduleSettingsRepository = specialistScheduleSettingsRepository;
		this.weeklyScheduleVersionRepository = weeklyScheduleVersionRepository;
		this.dateScheduleRepository = dateScheduleRepository;
		this.specialistService = specialistService;
		this.clock = clock;
	}

	@Transactional
	public void changeScheduleMode(Long specialistId, ScheduleMode mode) {
		if (specialistId == null) {
			throw new IllegalArgumentException("Specialist ID cannot be null");
		}
		if (mode == null) {
			throw new IllegalArgumentException("Mode cannot be null");
		}
		SpecialistScheduleSettings specialistSettings = specialistScheduleSettingsRepository
				.findForUpdateBySpecialistId(specialistId)
				.orElseThrow(() -> new IllegalStateException("Specialist settings not found"));
		specialistSettings.changeMode(mode, Instant.now(clock));
	}

	@Transactional
	public void changeBookingHorizon(Long specialistId, int horizonDays) {
		if (specialistId == null) {
			throw new IllegalArgumentException("Specialist ID cannot be null");
		}
		SpecialistScheduleSettings specialistSettings = specialistScheduleSettingsRepository
				.findForUpdateBySpecialistId(specialistId)
				.orElseThrow(() -> new IllegalStateException("Specialist settings not found"));
		specialistSettings.changeBookingHorizon(horizonDays, Instant.now(clock));
	}

	@Transactional
	public WeeklyScheduleVersion createWeeklyScheduleVersion(Long specialistId,
			CreateWeeklyScheduleVersionCommand command) {
		if (specialistId == null) {
			throw new IllegalArgumentException("Specialist ID cannot be null");
		}
		if (command == null) {
			throw new IllegalArgumentException("Command cannot be null");
		}
		if (command.effectiveFrom().isBefore(LocalDate.now(clock))) {
			throw new IllegalArgumentException("Effective from cannot be before now");
		}
		SpecialistScheduleSettings specialistSettings = specialistScheduleSettingsRepository
				.findForUpdateBySpecialistId(specialistId)
				.orElseThrow(() -> new IllegalStateException("Specialist schedule settings not found"));
		if (specialistSettings.getMode() == ScheduleMode.FLOATING) {
			throw new IllegalStateException("Weekly schedule versions are only allowed in REGULAR mode");
		}
		if (weeklyScheduleVersionRepository.existsVersionStartingOnOrAfter(specialistId, command.effectiveFrom())) {
			throw new IllegalStateException("A weekly schedule version already starts on or after the requested date");
		}
		Specialist spec = specialistService.getEligibleSpecialist(specialistId);
		WeeklyScheduleVersion version = new WeeklyScheduleVersion(spec, command.effectiveFrom(), Instant.now(clock));
		for (WeeklyWorkingIntervalCommand w : command.intervals()) {
			version.addInterval(new WeeklyWorkingInterval(w.dayOfWeek(), w.startTime(), w.endTime()));
		}
		Optional<WeeklyScheduleVersion> optVersion = weeklyScheduleVersionRepository.findEffectiveVersion(specialistId,
				command.effectiveFrom().minusDays(1));
		if (optVersion.isPresent()) {
			WeeklyScheduleVersion prevVersion = optVersion.get();
			prevVersion.shortenEffectiveUntil(command.effectiveFrom().minusDays(1));
		}
		return weeklyScheduleVersionRepository.save(version);
	}

	@Transactional
	public DateSchedule replaceDateSchedule(Long specialistId, LocalDate date, DateSchedulePurpose purpose,
			List<WorkingInterval> intervals) {
		if (specialistId == null) {
			throw new IllegalArgumentException("Specialist ID cannot be null");
		}
		if (date == null) {
			throw new IllegalArgumentException("Date cannot be null");
		}
		if (date.isBefore(LocalDate.now(clock))) {
			throw new IllegalArgumentException("Date cannot be before now");
		}
		if (purpose == null) {
			throw new IllegalArgumentException("Purpose cannot be null");
		}
		if (intervals == null) {
			throw new IllegalArgumentException("Intervals cannot be null");
		}
		SpecialistScheduleSettings specialistSettings = specialistScheduleSettingsRepository
				.findForUpdateBySpecialistId(specialistId)
				.orElseThrow(() -> new IllegalStateException("Specialist settings not found"));
		if (specialistSettings.getMode() == ScheduleMode.REGULAR && purpose == DateSchedulePurpose.MANUAL) {
			throw new IllegalArgumentException(
					"Incorrect combination: " + specialistSettings.getMode() + " and " + purpose);
		}
		if (specialistSettings.getMode() == ScheduleMode.FLOATING && purpose == DateSchedulePurpose.OVERRIDE) {
			throw new IllegalArgumentException(
					"Incorrect combination: " + specialistSettings.getMode() + " and " + purpose);
		}
		Optional<DateSchedule> optionalDateSchedule = dateScheduleRepository
				.findBySpecialistAndDateAndPurpose(specialistId, date, purpose);
		DateSchedule dateSchedule;
		if (optionalDateSchedule.isPresent()) {
			dateSchedule = optionalDateSchedule.get();
		} else {
			Specialist spec = specialistService.getEligibleSpecialist(specialistId);
			dateSchedule = new DateSchedule(spec, date, purpose, Instant.now(clock));
		}
		List<DateScheduleInterval> dateIntervals = intervals.stream()
				.map(interval -> new DateScheduleInterval(interval.startTime(), interval.endTime())).toList();
		dateSchedule.replaceIntervals(dateIntervals, Instant.now(clock));
		return dateScheduleRepository.save(dateSchedule);
	}

	@Transactional
	public void removeDateSchedule(Long specialistId, LocalDate date, DateSchedulePurpose purpose) {
		if (specialistId == null) {
			throw new IllegalArgumentException("Specialist ID cannot be null");
		}
		if (date == null) {
			throw new IllegalArgumentException("Date cannot be null");
		}
		if (date.isBefore(LocalDate.now(clock))) {
			throw new IllegalArgumentException("Date cannot be before now");
		}
		if (purpose == null) {
			throw new IllegalArgumentException("Purpose cannot be null");
		}
		specialistScheduleSettingsRepository.findForUpdateBySpecialistId(specialistId)
				.orElseThrow(() -> new IllegalStateException("Specialist settings not found"));
		DateSchedule dateSchedule = dateScheduleRepository
				.findBySpecialistAndDateAndPurpose(specialistId, date, purpose)
				.orElseThrow(() -> new IllegalStateException("Date schedule not found"));
		dateScheduleRepository.delete(dateSchedule);
	}
}
