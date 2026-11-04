package com.psyweb.schedule.service.model;

import java.time.LocalDate;
import java.util.List;

public record CreateWeeklyScheduleVersionCommand(LocalDate effectiveFrom,
		List<WeeklyWorkingIntervalCommand> intervals) {
	public CreateWeeklyScheduleVersionCommand {
		if (effectiveFrom == null) {
			throw new IllegalArgumentException("Effective from cannot be null");
		}
		if (intervals == null) {
			throw new IllegalArgumentException("Intervals cannot be null");
		}
		for (WeeklyWorkingIntervalCommand w : intervals) {
			if (w == null) {
				throw new IllegalArgumentException("Interval cannot be null");
			}
		}
		intervals = List.copyOf(intervals);
	}
}
