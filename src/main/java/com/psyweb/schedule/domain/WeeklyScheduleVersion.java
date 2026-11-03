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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "weekly_schedule_versions")
public class WeeklyScheduleVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "specialist_id", nullable = false)
	private Specialist specialist;

	@OneToMany(mappedBy = "scheduleVersion", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WeeklyWorkingInterval> intervals = new ArrayList<>();

	@Column(name = "effective_from", nullable = false)
	private LocalDate effectiveFrom;

	@Column(name = "effective_until")
	private LocalDate effectiveUntil;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected WeeklyScheduleVersion() {
	}

	public WeeklyScheduleVersion(Specialist specialist, LocalDate effectiveFrom, LocalDate effectiveUntil,
			Instant createdAt) {
		if (specialist == null) {
			throw new IllegalArgumentException("Specialist cannot be null");
		}
		if (effectiveFrom == null) {
			throw new IllegalArgumentException("Effective from cannot be null");
		}
		if (effectiveUntil != null && effectiveUntil.isBefore(effectiveFrom)) {
			throw new IllegalArgumentException("Effective until cannot be before effective from");
		}
		if (createdAt == null) {
			throw new IllegalArgumentException("Created time cannot be null");
		}
		this.specialist = specialist;
		this.effectiveFrom = effectiveFrom;
		this.effectiveUntil = effectiveUntil;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public LocalDate getEffectiveFrom() {
		return effectiveFrom;
	}

	public LocalDate getEffectiveUntil() {
		return effectiveUntil;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public List<WeeklyWorkingInterval> getIntervals() {
		return List.copyOf(intervals);
	}

	public void addInterval(WeeklyWorkingInterval interval) {
		if (interval == null) {
			throw new IllegalArgumentException("Interval cannot be null");
		}
		if (this.intervals.contains(interval)) {
			return;
		}
		for (WeeklyWorkingInterval i : this.intervals) {
			if (i.overlaps(interval)) {
				throw new IllegalArgumentException("Working interval overlaps with an existing interval");
			}
		}
		interval.assignTo(this);
		intervals.add(interval);
	}

	public void shortenEffectiveUntil(LocalDate newEffectiveUntil) {
		if (newEffectiveUntil == null) {
			throw new IllegalArgumentException("New effective until cannot be null");
		}
		if (newEffectiveUntil.isBefore(this.effectiveFrom)) {
			throw new IllegalArgumentException("Effective until cannot be before effective from");
		}
		if (this.effectiveUntil != null && newEffectiveUntil.isAfter(this.effectiveUntil)) {
			throw new IllegalArgumentException("Effective until cannot be extended");
		}
		if (newEffectiveUntil.equals(this.effectiveUntil)) {
			return;
		}
		this.effectiveUntil = newEffectiveUntil;
	}
}
