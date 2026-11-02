package com.psyweb.schedule.domain;

import java.time.Instant;

import com.psyweb.specialist.domain.Specialist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "specialist_schedule_settings")
public class SpecialistScheduleSettings {
	@Id
	private Long specialistId;
	
	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "specialist_id", nullable = false)
	private Specialist specialist;
	
	@Column(name = "mode", nullable = false)
	@Enumerated(EnumType.STRING)
	private ScheduleMode mode;
	
	@Column(name = "booking_horizon_days", nullable = false)
	private int bookingHorizonDays;
	
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
	
	protected SpecialistScheduleSettings() {}
	
	public SpecialistScheduleSettings(Specialist specialist, Instant createdAt) {
		this(specialist, 30, createdAt);
	}
	
	public SpecialistScheduleSettings(Specialist specialist, int bookingHorizonDays, Instant createdAt) {
		if (specialist == null) {
			throw new IllegalArgumentException("Specialist cannot be null");
		}
		if (createdAt == null) {
			throw new IllegalArgumentException("Creation time cannot be null");
		}
		if (bookingHorizonDays != 14 && bookingHorizonDays != 30 && bookingHorizonDays != 60 && bookingHorizonDays != 90) {
			throw new IllegalArgumentException("Booking horizon must be 14, 30, 60 or 90 days");
		}
		this.specialist = specialist;
		this.mode = ScheduleMode.REGULAR;
		this.bookingHorizonDays = bookingHorizonDays;
		this.updatedAt = createdAt;
	}
	
	public void changeMode(ScheduleMode newMode, Instant updatedAt) {
		if (updatedAt == null) {
			throw new IllegalArgumentException("Update time cannot be null");
		}
		if (newMode == null) {
			throw new IllegalArgumentException("Schedule mode cannot be null");
		}
		if (this.mode == newMode) {
			return;
		}
		this.mode = newMode;
		this.updatedAt = updatedAt;
	}

	public void changeBookingHorizon(int newHorizonDays, Instant updatedAt) {
		if (updatedAt == null) {
			throw new IllegalArgumentException("Update time cannot be null");
		}
		if (newHorizonDays != 14 && newHorizonDays != 30 && newHorizonDays != 60 && newHorizonDays != 90) {
			throw new IllegalArgumentException("Booking horizon must be 14, 30, 60 or 90 days");
		}
		if (this.bookingHorizonDays == newHorizonDays) {
			return;
		}
		this.bookingHorizonDays = newHorizonDays;
		this.updatedAt = updatedAt;
	}
}
