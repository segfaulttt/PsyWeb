package com.psyweb.schedule.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psyweb.schedule.domain.DateSchedule;
import com.psyweb.schedule.domain.DateScheduleInterval;
import com.psyweb.schedule.domain.DateSchedulePurpose;
import com.psyweb.schedule.domain.ScheduleMode;
import com.psyweb.schedule.domain.SpecialistScheduleSettings;
import com.psyweb.schedule.domain.WeeklyScheduleVersion;
import com.psyweb.schedule.repository.DateScheduleRepository;
import com.psyweb.schedule.repository.SpecialistScheduleSettingsRepository;
import com.psyweb.schedule.repository.WeeklyScheduleVersionRepository;
import com.psyweb.schedule.service.model.WorkingInterval;

@Transactional(readOnly = true)
@Service
public class ScheduleQueryService {
	private final SpecialistScheduleSettingsRepository specialistScheduleSettingsRepository;
	private final DateScheduleRepository dateScheduleRepository;
	private final WeeklyScheduleVersionRepository weeklyScheduleVersionRepository;

	public ScheduleQueryService(SpecialistScheduleSettingsRepository specialistScheduleSettingsRepository,
			DateScheduleRepository dateScheduleRepository,
			WeeklyScheduleVersionRepository weeklyScheduleVersionRepository) {
		this.specialistScheduleSettingsRepository = specialistScheduleSettingsRepository;
		this.dateScheduleRepository = dateScheduleRepository;
		this.weeklyScheduleVersionRepository = weeklyScheduleVersionRepository;
	}

	public SpecialistScheduleSettings getScheduleSettings(Long specialistId) {
		if (specialistId == null) {
			throw new IllegalArgumentException("Specialist ID cannot be null");
		}

		return specialistScheduleSettingsRepository.findById(specialistId)
				.orElseThrow(() -> new IllegalStateException("Specialist schedule settings not found for specialist: " + specialistId));
	}

	public List<WorkingInterval> getEffectiveWorkingIntervals(Long specialistId, LocalDate date) {
		if (specialistId == null) {
			throw new IllegalArgumentException("Specialist ID cannot be null");
		}
		if (date == null) {
			throw new IllegalArgumentException("Date cannot be null");
		}
		SpecialistScheduleSettings specialistSettings = getScheduleSettings(specialistId);

		if (specialistSettings.getMode() == ScheduleMode.REGULAR) {
			Optional<DateSchedule> optional = dateScheduleRepository.findBySpecialistAndDateAndPurpose(specialistId,
					date, DateSchedulePurpose.OVERRIDE);
			if (optional.isEmpty()) {
				Optional<WeeklyScheduleVersion> optionalWeeklyVersion = weeklyScheduleVersionRepository
						.findEffectiveVersion(specialistId, date);
				if (optionalWeeklyVersion.isEmpty()) {
					return List.of();
				} else {
					WeeklyScheduleVersion weeklyVersion = optionalWeeklyVersion.get();
					return weeklyVersion.getIntervals().stream()
							.filter(interval -> interval.getDayOfWeek() == date.getDayOfWeek())
							.map(interval -> new WorkingInterval(interval.getStartTime(), interval.getEndTime()))
							.sorted(Comparator.comparing(WorkingInterval::startTime))
							.toList();
				}
			}
			DateSchedule dateSchedule = optional.get();
			List<DateScheduleInterval> intervals = dateSchedule.getIntervals();
			return intervals.stream()
					.map(interval -> new WorkingInterval(interval.getStartTime(), interval.getEndTime()))
					.sorted(Comparator.comparing(WorkingInterval::startTime))
					.toList();
		}
		Optional<DateSchedule> optional = dateScheduleRepository.findBySpecialistAndDateAndPurpose(specialistId, date,
				DateSchedulePurpose.MANUAL);
		if (optional.isEmpty()) {
			return List.of();
		}
		DateSchedule dateSchedule = optional.get();
		List<DateScheduleInterval> intervals = dateSchedule.getIntervals();
		return intervals.stream()
				.map(interval -> new WorkingInterval(interval.getStartTime(), interval.getEndTime()))
				.sorted(Comparator.comparing(WorkingInterval::startTime))
				.toList();
	}
}