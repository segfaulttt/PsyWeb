package com.psyweb.availability.domain;

import java.time.LocalDateTime;

import com.psyweb.specialist.domain.Specialist;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "slots")
public class AvailabilitySlot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "specialist_id", nullable = false)
	private Specialist specialist;
	
	@Column(name = "start_time", nullable = false)
	private LocalDateTime startTime;
	
	@Column(name = "end_time", nullable = false)
	private LocalDateTime endTime;
	
	@Column(name = "availability_status", nullable = false)
	@Enumerated(EnumType.STRING)
	private AvailabilityStatus availabilityStatus;
	
	protected AvailabilitySlot() {}
	
	public AvailabilitySlot(Specialist specialist, LocalDateTime startTime, LocalDateTime endTime) {
		this.specialist = specialist;
		if (startTime == null || endTime == null) {
		    throw new IllegalArgumentException("Time cannot be null");
		}

		if (!startTime.isBefore(endTime)) {
		    throw new IllegalArgumentException("Start must be before end");
		}
		this.startTime = startTime;
		this.endTime = endTime;
		this.availabilityStatus = AvailabilityStatus.FREE;
	}
	
	public Long getId() {
		return this.id;
	}
	
	public Long getSpecialistId() {
		return specialist.getId();
	}
	
	public LocalDateTime getStartTime() {
		return this.startTime;
	}
	
	public LocalDateTime getEndTime() {
		return this.endTime;
	}
	
	public AvailabilityStatus getAvailabilityStatus() {
		return this.availabilityStatus;
	}
	
	public void markBlocked() {
		this.availabilityStatus = AvailabilityStatus.BLOCKED;
	}
	
	public void markFree() {
		this.availabilityStatus = AvailabilityStatus.FREE;
	}
	
	public void markBooked() {
		this.availabilityStatus = AvailabilityStatus.BOOKED;
	}
}
