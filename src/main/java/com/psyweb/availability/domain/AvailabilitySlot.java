package com.psyweb.availability.domain;

import java.time.LocalDateTime;

import com.psyweb.availability.exception.InvalidAvailabilitySlotDataException;
import com.psyweb.availability.exception.InvalidAvailabilitySlotStateException;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
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
	
	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;
	
	@Column(name = "cancellation_initiator")
	@Enumerated(EnumType.STRING)
	private CancellationInitiator initiator;
	
	@Column(name = "cancellation_reason")
	@Enumerated(EnumType.STRING)
	private CancellationReason reason;
	
	protected AvailabilitySlot() {}
	
	public AvailabilitySlot(Specialist specialist, LocalDateTime startTime, LocalDateTime endTime) {
		if (startTime == null || endTime == null) {
		    throw new InvalidAvailabilitySlotDataException("Time cannot be null");
		}

		if (!startTime.isBefore(endTime)) {
		    throw new InvalidAvailabilitySlotDataException("Start must be before end");
		}
		if (specialist == null) {
			throw new InvalidAvailabilitySlotDataException("Specialist cannot be null");
		}
		this.specialist = specialist;
		this.startTime = startTime;
		this.endTime = endTime;
		this.availabilityStatus = AvailabilityStatus.FREE;
	}
	
	private void validateCancellation(LocalDateTime cancelledAt, CancellationInitiator initiator, CancellationReason reason) {
		if (cancelledAt == null || initiator == null || reason == null) {
			throw new InvalidAvailabilitySlotDataException("Cancellation metadata cannot be null");
		}
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
	
	public LocalDateTime getCancelledAt() {
		return this.cancelledAt;
	}
	
	public CancellationInitiator getCancellationInitiator() {
		return this.initiator;
	}
	
	public CancellationReason getCancellationReason() {
		return this.reason;
	}
	
	public void reserve() {
		if (availabilityStatus != AvailabilityStatus.FREE) {
			throw new InvalidAvailabilitySlotStateException("Cannot reserve slot with status " + availabilityStatus);
		}
		this.availabilityStatus = AvailabilityStatus.RESERVED;
	}
	
	public void releaseReservation() {
		if (availabilityStatus != AvailabilityStatus.RESERVED) {
			throw new InvalidAvailabilitySlotStateException("Cannot release reservation from slot with status " + availabilityStatus);
		}
		this.availabilityStatus = AvailabilityStatus.FREE;
	}
	
	public void confirmBooking() {
		if (availabilityStatus != AvailabilityStatus.RESERVED) {
			throw new InvalidAvailabilitySlotStateException("Cannot confirm booking for slot with status " + availabilityStatus);
		}
		this.availabilityStatus = AvailabilityStatus.BOOKED;
	}
	
	public void releaseBooking() {
		if (availabilityStatus != AvailabilityStatus.BOOKED) {
			throw new InvalidAvailabilitySlotStateException("Cannot release booking from slot with status " + availabilityStatus);
		}
		this.availabilityStatus = AvailabilityStatus.FREE;
	}
	
	public void cancel(LocalDateTime cancelledAt, CancellationInitiator initiator, CancellationReason reason) {
		if (availabilityStatus == AvailabilityStatus.CANCELLED) {
			throw new InvalidAvailabilitySlotStateException("Cannot cancel slot with status " + availabilityStatus);
		}
		validateCancellation(cancelledAt, initiator, reason);
		this.availabilityStatus = AvailabilityStatus.CANCELLED;
		this.cancelledAt = cancelledAt;
		this.initiator = initiator;
		this.reason = reason;
	}
}
