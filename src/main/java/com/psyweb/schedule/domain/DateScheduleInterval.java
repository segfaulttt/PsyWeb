package com.psyweb.schedule.domain;

import java.time.Duration;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "date_schedule_intervals")
public class DateScheduleInterval {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "date_schedule_id", nullable = false)
	private DateSchedule dateSchedule;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

	protected DateScheduleInterval() {
	}

	public DateScheduleInterval(LocalTime startTime, LocalTime endTime) {
		if (startTime == null || endTime == null) {
			throw new IllegalArgumentException("Start and end time cannot be null");
		}
		if (!startTime.isBefore(endTime)) {
			throw new IllegalArgumentException("Start time must be before end time");
		}

		this.startTime = startTime;
		this.endTime = endTime;
	}

	void validateAssignmentTo(DateSchedule schedule) {
		if (schedule == null) {
			throw new IllegalArgumentException("Date schedule cannot be null");
		}

		if (this.dateSchedule != null && this.dateSchedule != schedule) {
			throw new IllegalStateException("Interval already belongs to another date schedule");
		}
	}

	public UUID getId() {
		return id;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	void assignTo(DateSchedule schedule) {
	    validateAssignmentTo(schedule);
	    this.dateSchedule = schedule;
	}

	public boolean overlaps(DateScheduleInterval other) {
		if (other == null) {
			throw new IllegalArgumentException("Other interval cannot be null");
		}
		return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
	}

	public Duration duration() {
		return Duration.between(startTime, endTime);
	}

	void detachFrom(DateSchedule schedule) {
		if (this.dateSchedule != schedule) {
			throw new IllegalStateException("Interval does not belong to this date schedule");
		}

		this.dateSchedule = null;
	}
	
	public boolean hasSameTimeRange(DateScheduleInterval other) {
		if (other == null) {
			return false;
		}
		return this.startTime.equals(other.startTime) && this.endTime.equals(other.endTime);
	}
}
