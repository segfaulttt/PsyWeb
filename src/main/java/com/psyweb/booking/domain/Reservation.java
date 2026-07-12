package com.psyweb.booking.domain;

import java.time.LocalDateTime;

import com.psyweb.availability.domain.AvailabilitySlot;
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
	
	protected Reservation() {}
	
	public Reservation(User client, AvailabilitySlot slot, LocalDateTime expiresAt) {
		if (client == null) {
			throw new IllegalArgumentException("Incorrect client");
		}
		if (slot == null) {
			throw new IllegalArgumentException("Incorrect slot");
		}
		this.createdAt = LocalDateTime.now();
		if (expiresAt == null || !expiresAt.isAfter(this.createdAt)) {
			throw new IllegalArgumentException("Incorrect expires time");
		}		
		this.client = client;
		this.slot = slot;
		this.status = ReservationStatus.ACTIVE;
		this.expiresAt = expiresAt;
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
	
	public void expire() {
		if (this.status != ReservationStatus.ACTIVE || this.expiresAt.isAfter(LocalDateTime.now())) {
			throw new IllegalArgumentException("Cannot mark expired");
		}
		this.status = ReservationStatus.EXPIRED;
	}
	
	public boolean isExpired() {
		if (this.status == ReservationStatus.ACTIVE || !this.expiresAt.isAfter(LocalDateTime.now())) {
			return true;
		}
		return false;
	}
	
	public void cancel() {
		if (this.status != ReservationStatus.ACTIVE) {
			throw new IllegalArgumentException("Cannot mark cancelled");
		}
		this.status = ReservationStatus.CANCELLED;
	}
	
	public void confirm() {
		if (this.status != ReservationStatus.ACTIVE || !this.expiresAt.isAfter(LocalDateTime.now())) {
			throw new IllegalArgumentException("Cannot mark confirm");
		}
		this.status = ReservationStatus.CONFIRMED;
	}
}
