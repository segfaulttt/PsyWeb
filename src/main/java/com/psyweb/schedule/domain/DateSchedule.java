package com.psyweb.schedule.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.psyweb.specialist.domain.Specialist;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "date_schedules")
public class DateSchedule {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "specialist_id", nullable = false)
	private Specialist specialist;

	@Column(name = "schedule_date", nullable = false)
	private LocalDate date;

	@Column(name = "purpose", nullable = false)
	@Enumerated(EnumType.STRING)
	private DateSchedulePurpose purpose;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "dateSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<DateScheduleInterval> intervals = new ArrayList<>();

	protected DateSchedule() {
	}

	public DateSchedule(Specialist specialist, LocalDate date, DateSchedulePurpose purpose, Instant createdAt) {
		if (specialist == null) {
			throw new IllegalArgumentException("Specialist cannot be null");
		}
		if (date == null) {
			throw new IllegalArgumentException("Schedule date cannot be null");
		}
		if (purpose == null) {
			throw new IllegalArgumentException("Schedule purpose cannot be null");
		}
		if (createdAt == null) {
			throw new IllegalArgumentException("Creation time cannot be null");
		}

		this.specialist = specialist;
		this.date = date;
		this.purpose = purpose;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public LocalDate getScheduleDate() {
		return date;
	}

	public DateSchedulePurpose getPurpose() {
		return purpose;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public List<DateScheduleInterval> getIntervals() {
		return List.copyOf(intervals);
	}

	public void addInterval(DateScheduleInterval interval) {
		if (interval == null) {
			throw new IllegalArgumentException("Interval cannot be null");
		}
		if (this.intervals.contains(interval)) {
			return;
		}

		for (DateScheduleInterval in : intervals) {
			if (in.overlaps(interval)) {
				throw new IllegalArgumentException("Working interval overlaps with an existing interval");
			}
		}
		interval.assignTo(this);
		this.intervals.add(interval);
	}

	public boolean isClosed() {
		return intervals.isEmpty();
	}

	public void clearIntervals(Instant updatedAt) {
	    replaceIntervals(List.of(), updatedAt);
	}

	public void replaceIntervals(List<DateScheduleInterval> newIntervals, Instant updatedAt) {
		if (newIntervals == null) {
			throw new IllegalArgumentException("New intervals cannot be null");
		}
		if (updatedAt == null) {
			throw new IllegalArgumentException("Update time cannot be null");
		}
		if (updatedAt.isBefore(this.updatedAt)) {
			throw new IllegalArgumentException("Update time cannot be before previous update time");
		}
		for (DateScheduleInterval d : newIntervals) {
			if (d == null) {
				throw new IllegalArgumentException("Interval cannot be null");
			}
		}
		for (int i = 0; i < newIntervals.size(); i++) {
			DateScheduleInterval d = newIntervals.get(i);
			for (int j = i + 1; j < newIntervals.size(); j++) {
				DateScheduleInterval dsh = newIntervals.get(j);
				if (d.overlaps(dsh)) {
					throw new IllegalArgumentException("Working intervals must not overlap");
				}
			}
		}

		for (DateScheduleInterval interval : newIntervals) {
			interval.validateAssignmentTo(this);
		}

		boolean unchanged = this.intervals.size() == newIntervals.size();

		if (unchanged) {
			for (DateScheduleInterval interval : newIntervals) {
				if (!containsSameTimeRange(this.intervals, interval)) {
					unchanged = false;
					break;
				}
			}
		}

		if (unchanged) {
			return;
		}

		for (DateScheduleInterval interval : List.copyOf(this.intervals)) {
			if (!containsSameTimeRange(newIntervals, interval)) {
				interval.detachFrom(this);
				this.intervals.remove(interval);
			}
		}

		for (DateScheduleInterval interval : newIntervals) {
			if (!containsSameTimeRange(this.intervals, interval)) {
				interval.assignTo(this);
				this.intervals.add(interval);
			}
		}

		this.updatedAt = updatedAt;
	}

	private boolean containsSameTimeRange(List<DateScheduleInterval> intervals, DateScheduleInterval target) {
		for (DateScheduleInterval interval : intervals) {
			if (interval.hasSameTimeRange(target)) {
				return true;
			}
		}
		return false;
	}
}
