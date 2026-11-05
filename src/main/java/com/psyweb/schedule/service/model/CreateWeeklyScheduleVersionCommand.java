
package com.psyweb.schedule.service.model;

import java.time.LocalDate;
import java.util.List;

public record CreateWeeklyScheduleVersionCommand(LocalDate effectiveFrom, LocalDate effectiveUntil,
		List<WeeklyWorkingIntervalCommand> intervals) {

	public CreateWeeklyScheduleVersionCommand {
		if (effectiveFrom == null) {
			throw new IllegalArgumentException("Effective from cannot be null");
		}

		if (effectiveUntil != null && effectiveUntil.isBefore(effectiveFrom)) {
			throw new IllegalArgumentException("Effective until cannot be before effective from");
		}

		if (intervals == null) {
			throw new IllegalArgumentException("Intervals cannot be null");
		}

		for (WeeklyWorkingIntervalCommand interval : intervals) {
			if (interval == null) {
				throw new IllegalArgumentException("Interval cannot be null");
			}
		}

		intervals = List.copyOf(intervals);
	}

	public CreateWeeklyScheduleVersionCommand(LocalDate effectiveFrom, List<WeeklyWorkingIntervalCommand> intervals) {
		this(effectiveFrom, null, intervals);
	}
}
