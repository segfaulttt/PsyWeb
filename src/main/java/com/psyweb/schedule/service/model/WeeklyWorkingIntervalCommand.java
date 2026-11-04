package com.psyweb.schedule.service.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record WeeklyWorkingIntervalCommand(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
	public WeeklyWorkingIntervalCommand{
		if (dayOfWeek == null) {
			throw new IllegalArgumentException("Day of week cannot be null");
		}
		if (startTime == null || endTime == null) {
			throw new IllegalArgumentException("Start and end time cannot be null");	
		}
		if (!startTime.isBefore(endTime)) {
			throw new IllegalArgumentException("Start time must be before end time");
		}
	}
}
