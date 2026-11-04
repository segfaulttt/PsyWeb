package com.psyweb.schedule.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

		return specialistScheduleSettingsRepository.findById(specialistId).orElseThrow(() -> new IllegalStateException(
				"Specialist schedule settings not found for specialist: " + specialistId));
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
							.sorted(Comparator.comparing(WorkingInterval::startTime)).toList();
				}
			}
			DateSchedule dateSchedule = optional.get();
			List<DateScheduleInterval> intervals = dateSchedule.getIntervals();
			return convertWorkingIntervals(intervals);
		}
		Optional<DateSchedule> optional = dateScheduleRepository.findBySpecialistAndDateAndPurpose(specialistId, date,
				DateSchedulePurpose.MANUAL);
		if (optional.isEmpty()) {
			return List.of();
		}
		DateSchedule dateSchedule = optional.get();
		List<DateScheduleInterval> intervals = dateSchedule.getIntervals();
		return convertWorkingIntervals(intervals);
	}

	public Map<LocalDate, List<WorkingInterval>> getEffectiveWorkingIntervals(Long specialistId, LocalDate fromDate,
			LocalDate toDate) {
		if (specialistId == null) {
			throw new IllegalArgumentException("Specialist ID cannot be null");
		}
		if (fromDate == null || toDate == null) {
			throw new IllegalArgumentException("Dates cannot be null");
		}
		if (fromDate.isAfter(toDate)) {
			throw new IllegalArgumentException("From date must be before to date");
		}
		SpecialistScheduleSettings specialistSettings = getScheduleSettings(specialistId);
		if (specialistSettings.getMode() == ScheduleMode.FLOATING) {
			List<DateSchedule> dateSchedules = dateScheduleRepository
					.findBySpecialistAndDateRange(specialistId, fromDate, toDate).stream()
					.filter(dateSchedule -> dateSchedule.getPurpose() == DateSchedulePurpose.MANUAL).toList();
			Map<LocalDate, List<WorkingInterval>> workingIntervals = new HashMap<>();

			for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
				workingIntervals.put(date, List.of());
			}

			for (DateSchedule d : dateSchedules) {
				workingIntervals.put(d.getScheduleDate(), convertWorkingIntervals(d.getIntervals()));
			}
			return workingIntervals;
		}

		List<WeeklyScheduleVersion> weeklyVersions = weeklyScheduleVersionRepository.findOverlappingPeriod(specialistId,
				fromDate, toDate);
		List<DateSchedule> overrides = dateScheduleRepository
				.findBySpecialistAndDateRange(specialistId, fromDate, toDate).stream()
				.filter(schedule -> schedule.getPurpose() == DateSchedulePurpose.OVERRIDE).toList();

		Map<LocalDate, List<WorkingInterval>> workingIntervals = new HashMap<>();
		
		for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
			LocalDate currentDate = date;
			Optional<WeeklyScheduleVersion> optionalVersion = weeklyVersions.stream().filter(v -> !v.getEffectiveFrom().isAfter(currentDate)
					&& (v.getEffectiveUntil() == null || !v.getEffectiveUntil().isBefore(currentDate))).findFirst();
			List<WorkingInterval> intervals = List.of();

			if (optionalVersion.isPresent()) {
			    WeeklyScheduleVersion version = optionalVersion.get();

			    intervals = version.getIntervals().stream()
			            .filter(interval -> interval.getDayOfWeek() == currentDate.getDayOfWeek())
			            .map(interval -> new WorkingInterval(
			                    interval.getStartTime(),
			                    interval.getEndTime()))
			            .sorted(Comparator.comparing(WorkingInterval::startTime))
			            .toList();
			}
			Optional<DateSchedule> optionalDataSchedule = overrides.stream().filter(o -> o.getScheduleDate().equals(currentDate)).findFirst();
			if (optionalDataSchedule.isPresent()) {
				DateSchedule schedule = optionalDataSchedule.get();
				intervals = convertWorkingIntervals(schedule.getIntervals());
			}
			workingIntervals.put(currentDate, intervals);
		}
		return workingIntervals;
	}

	List<WorkingInterval> convertWorkingIntervals(List<DateScheduleInterval> intervals) {
		return intervals.stream().map(interval -> new WorkingInterval(interval.getStartTime(), interval.getEndTime()))
				.sorted(Comparator.comparing(WorkingInterval::startTime)).toList();
	}

}