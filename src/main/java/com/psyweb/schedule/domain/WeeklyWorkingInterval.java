package com.psyweb.schedule.domain;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.psyweb.schedule.persistence.DayOfWeekConverter;

@Entity
@Table(name = "weekly_working_intervals")
public class WeeklyWorkingInterval {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "schedule_version_id", nullable = false)
	private WeeklyScheduleVersion scheduleVersion;

	@Convert(converter = DayOfWeekConverter.class)
	@Column(name = "day_of_week", nullable = false)
	private DayOfWeek dayOfWeek;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

	protected WeeklyWorkingInterval() {
	}

	public WeeklyWorkingInterval(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
		if (dayOfWeek == null) {
			throw new IllegalArgumentException("Day of week cannot be null");
		}
		if (startTime == null || endTime == null) {
			throw new IllegalArgumentException("Start and end time cannot be null");
		}
		if (!startTime.isBefore(endTime)) {
			throw new IllegalArgumentException("Start time must be before end time");
		}
		this.dayOfWeek = dayOfWeek;
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public UUID getId() {
		return id;
	}
	
	public DayOfWeek getDayOfWeek() {
		return dayOfWeek;
	}
	
	public LocalTime getStartTime() {
		return startTime;
	}
	
	public LocalTime getEndTime() {
		return endTime;
	}

	void assignTo(WeeklyScheduleVersion version) {
		if (version == null) {
			throw new IllegalArgumentException("Version cannot be null");
		}
		if (this.scheduleVersion != null && this.scheduleVersion != version) {
			throw new IllegalStateException("Interval already belongs to another schedule version");
		}
		this.scheduleVersion = version;
	}
	
	public boolean overlaps(WeeklyWorkingInterval other) {
		if (other == null) {
			throw new IllegalArgumentException("Other interval cannot be null");
		}
		if (this.dayOfWeek != other.dayOfWeek) {
		    return false;
		}
		return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
	}
	
	public Duration duration() {
		return Duration.between(startTime, endTime);
	}
}
