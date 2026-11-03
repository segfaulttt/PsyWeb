package com.psyweb.schedule.domain;

import java.time.Instant;
import java.time.ZoneId;

import com.psyweb.schedule.persistence.ZoneIdConverter;
import com.psyweb.specialist.domain.Specialist;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "specialist_time_zone_guards")
public class SpecialistTimeZoneGuard {

	@Id
	private Long specialistId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "specialist_id", nullable = false)
	private Specialist specialist;

	@Column(name = "time_zone", nullable = false, length = 100)
	@Convert(converter = ZoneIdConverter.class)
	private ZoneId timeZone;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected SpecialistTimeZoneGuard() {
	}

	public SpecialistTimeZoneGuard(Specialist specialist, ZoneId timeZone, Instant createdAt) {
		if (specialist == null) {
			throw new IllegalArgumentException("Specialist cannot be null");
		}
		if (timeZone == null) {
			throw new IllegalArgumentException("Time zone cannot be null");
		}
		if (createdAt == null) {
			throw new IllegalArgumentException("Creation time cannot be null");
		}
		this.specialist = specialist;
		this.timeZone = timeZone;
		this.updatedAt = createdAt;
	}

	public Long getSpecialistId() {
		return specialistId;
	}

	public ZoneId getTimeZone() {
		return timeZone;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void changeTimeZone(ZoneId newTimeZone, Instant updatedAt) {
		if (newTimeZone == null) {
			throw new IllegalArgumentException("Time zone cannot be null");
		}
		if (updatedAt == null) {
			throw new IllegalArgumentException("Update time cannot be null");
		}
		if (updatedAt.isBefore(this.updatedAt)) {
			throw new IllegalArgumentException("Update time cannot be before previous update time");
		}
		
		if (this.timeZone.equals(newTimeZone)) {
			return;
		}
		this.timeZone = newTimeZone;
		this.updatedAt = updatedAt;
	}
}
