package com.psyweb.booking.domain;

import java.time.LocalDateTime;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.user.domain.User;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "bookings")
public class Booking {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "client_id", nullable = false)
	// later: client entity
	private User client;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "specialist_id", nullable = false)
	private Specialist specialist;
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "slot_id", nullable = false)
	private AvailabilitySlot slot;
	
	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private BookingStatus status;
	
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
	
	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;
	
	protected Booking() {}
	
	public Booking(User client, Specialist specialist, AvailabilitySlot slot) {
		if (client == null) {
			throw new IllegalArgumentException("Client cannot be blank");
		}
		if (specialist == null) {
			throw new IllegalArgumentException("Specialist cannot be blank");
		}
		if (slot == null) {
			throw new IllegalArgumentException("Slot cannot be blank");
		}
		this.createdAt = LocalDateTime.now();
		this.client = client;
		this.specialist = specialist;
		this.slot = slot;
		this.status = BookingStatus.CONFIRMED;
	}
	
	public Long getId() {
		return this.id;
	}
	
	public Long getClientId() {
		return this.client.getId();
	}
	
	public Long getSpecialistId() {
		return this.specialist.getId();
	}
	
	public Long getSlotId() {
		return this.slot.getId();
	}
	
	public BookingStatus getStatus() {
		return this.status;
	}
	
	public LocalDateTime getCreatedAt() {
		return this.createdAt;
	}
	
	public LocalDateTime getCancelledAt() {
		return this.cancelledAt;
	}
	
	public void cancel(LocalDateTime time) {
		if (this.status != BookingStatus.CONFIRMED) {
			throw new IllegalArgumentException("Cannot cancel booking");
		}
		if (time == null || !time.isAfter(this.createdAt)) {
			throw new IllegalArgumentException("Invalid cancellation time");
		}
		this.cancelledAt = time;
		this.status = BookingStatus.CANCELLED;
	}
	
	public void complete() {
		if (this.status != BookingStatus.CONFIRMED) {
			throw new IllegalArgumentException("Only confirmed booking can be completed");
		}
		this.status = BookingStatus.COMPLETED;
	}
	
	public void markNoShow() {
		if (this.status != BookingStatus.CONFIRMED) {
			throw new IllegalArgumentException("Cannot mark no show booking");
		}
		this.status = BookingStatus.NO_SHOW;
	}	
}
