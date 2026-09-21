package com.psyweb.booking.domain;

import java.time.LocalDateTime;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.booking.exception.InvalidReservationDataException;
import com.psyweb.booking.exception.InvalidReservationStateException;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
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
import jakarta.persistence.Table;

@Entity
@Table(name = "reservations")
public class Reservation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "client_id", nullable = false)
	private User client;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "slot_id", nullable = false)
	private AvailabilitySlot slot;

	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private ReservationStatus status;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;
	
	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;
	
	@Column(name = "cancellation_initiator")
	@Enumerated(EnumType.STRING)
	private CancellationInitiator initiator;
	
	@Column(name = "cancellation_reason")
	@Enumerated(EnumType.STRING)
	private CancellationReason reason;
	
	protected Reservation() {}
	
	public Reservation(User client, AvailabilitySlot slot, LocalDateTime createdAt, LocalDateTime expiresAt) {
		if (client == null) {
	        throw new InvalidReservationDataException("Incorrect client");
	    }
	    if (slot == null) {
	        throw new InvalidReservationDataException("Slot cannot be null");
	    }
	    if (createdAt == null) {
	        throw new InvalidReservationDataException("Created time cannot be null");
	    }
	    if (expiresAt == null || !expiresAt.isAfter(createdAt)) {
	        throw new InvalidReservationDataException("Incorrect expires time");
	    }

	    this.client = client;
	    this.slot = slot;
	    this.status = ReservationStatus.ACTIVE;
	    this.createdAt = createdAt;
	    this.expiresAt = expiresAt;
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
	
	public Long getSlotId() {
		return this.slot.getId();
	}
	
	public ReservationStatus getStatus() {
		return this.status;
	}
	
	public LocalDateTime getCreatedAt() {
		return this.createdAt;
	}
	
	public LocalDateTime getExpiresAt() {
		return this.expiresAt;
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
	
	public void expire(LocalDateTime now) {
	    if (!isExpired(now)) {
	        throw new InvalidReservationStateException("Cannot mark expired");
	    }

	    status = ReservationStatus.EXPIRED;
	}
	
	public boolean isExpired(LocalDateTime now) {
	    return status == ReservationStatus.ACTIVE && !expiresAt.isAfter(now);
	}
	
	public void cancel(LocalDateTime cancelledAt, CancellationInitiator initiator, CancellationReason reason) {
		validateCancellation(cancelledAt, initiator, reason);
		if (this.status != ReservationStatus.ACTIVE) {
			throw new InvalidReservationStateException("Cannot mark cancelled");
		}
		this.status = ReservationStatus.CANCELLED;
		this.cancelledAt = cancelledAt;
		this.initiator = initiator;
		this.reason = reason;
	}
	
	public void confirm(LocalDateTime now) {
	    if (status != ReservationStatus.ACTIVE || !expiresAt.isAfter(now)) {
	        throw new InvalidReservationStateException("Cannot mark confirm");
	    }

	    status = ReservationStatus.CONFIRMED;
	}
}
