package com.psyweb.booking.domain;

import java.time.LocalDateTime;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.booking.exception.InvalidBookingDataException;
import com.psyweb.booking.exception.InvalidBookingStateException;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
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
	private User client;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "specialist_id", nullable = false)
	private Specialist specialist;
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "slot_id", nullable = false)
	private AvailabilitySlot slot;
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id")
	private Reservation reservation;
	
	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private BookingStatus status;
	
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
	
	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;
	
	@Column(name = "cancellation_initiator")
	@Enumerated(EnumType.STRING)
	private CancellationInitiator initiator;
	
	@Column(name = "cancellation_reason")
	@Enumerated(EnumType.STRING)
	private CancellationReason reason;
	
	protected Booking() {}
	
	public Booking(User client, Specialist specialist, AvailabilitySlot slot, Reservation reservation, LocalDateTime createdAt) {
		if (client == null) {
			throw new InvalidBookingDataException("Client cannot be blank");
		}
		if (specialist == null) {
			throw new InvalidBookingDataException("Specialist cannot be blank");
		}
		if (slot == null) {
			throw new InvalidBookingDataException("Slot cannot be blank");
		}
		if (reservation == null || reservation.getStatus() != ReservationStatus.CONFIRMED) {
			throw new InvalidBookingDataException("Reservation cannot be blank");
		}
		if (createdAt == null) {
			throw new InvalidBookingDataException("Creation time cannot be blank");
		}
		this.createdAt = createdAt;
		this.client = client;
		this.specialist = specialist;
		this.slot = slot;
		this.reservation = reservation;
		this.status = BookingStatus.CONFIRMED;
	}
	
	private void validateCancellation(LocalDateTime cancelledAt, CancellationInitiator initiator, CancellationReason reason) {
		if (cancelledAt == null || initiator == null || reason == null) {
			throw new IllegalArgumentException("Invaid cancellation parametr");
		}
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
	
	public Long getReservationId() {
		return this.reservation.getId();
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
	
	public CancellationInitiator getCancellationInitiator() {
		return this.initiator;
	}
	
	public CancellationReason getCancellationReason() {
		return this.reason;
	}
	
	public void cancel(LocalDateTime time, CancellationInitiator initiator, CancellationReason reason) {
		if (this.status != BookingStatus.CONFIRMED) {
			throw new InvalidBookingStateException("Cannot cancel booking");
		}
		validateCancellation(time, initiator, reason);
		
		if (!time.isAfter(this.createdAt)) {
			throw new InvalidBookingDataException("Invalid cancellation time");
		}
		this.cancelledAt = time;
		this.status = BookingStatus.CANCELLED;
		this.initiator = initiator;
		this.reason = reason;
	}
	
	public void complete() {
		if (this.status != BookingStatus.CONFIRMED) {
			throw new InvalidBookingStateException("Only confirmed booking can be completed");
		}
		this.status = BookingStatus.COMPLETED;
	}
	
	public void markNoShow() {
		if (this.status != BookingStatus.CONFIRMED) {
			throw new InvalidBookingStateException("Cannot mark no show booking");
		}
		this.status = BookingStatus.NO_SHOW;
	}	
}
