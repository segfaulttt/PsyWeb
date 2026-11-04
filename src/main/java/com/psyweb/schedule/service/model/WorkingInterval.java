package com.psyweb.schedule.service.model;

import java.time.LocalTime;

public record WorkingInterval(LocalTime startTime, LocalTime endTime) {
	public WorkingInterval {
		if (startTime == null || endTime == null) {
			throw new IllegalArgumentException("Start and end time cannot be null");
		}
		if (!endTime.isAfter(startTime)) {
			throw new IllegalArgumentException("Start time must be before end time");
		}
	}
}
