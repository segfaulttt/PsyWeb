package com.psyweb.auth.domain;

import java.time.Instant;
import java.util.UUID;

import com.psyweb.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_sessions")
public class RefreshSession {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
	
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
	
	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;
	
	@Column(name = "revoked_at")
	private Instant revokedAt;
	
	@Column(name = "last_used_at")
	private Instant lastUsedAt;
	
	protected RefreshSession() {}
	
	public RefreshSession(User user, Instant createdAt, Instant expiresAt) {
		if (user == null) {
			throw new IllegalArgumentException("User cannot be null");
		}
		if (createdAt == null || expiresAt == null) {
			throw new IllegalArgumentException("Time cannot be null");
		}
		if (!createdAt.isBefore(expiresAt)) {
			throw new IllegalArgumentException("Creation time must be strictly earlier than the expiration time");
		}
		
		this.user = user;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}
	
	public Instant getCreatedAt() {
		return this.createdAt;
	}
	
	public Instant getExpiresAt() {
		return this.expiresAt;
	}
	
	public Long getUserId() {
		return user.getId();
	}
	
	public UUID getId() {
		return this.id;
	}
	
	public Instant getRevokedAt() {
		return this.revokedAt;
	}
	
	public Instant getLastUsedAt() {
		return this.lastUsedAt;
	}
	
	public void addRevokedAt(Instant revokedAt) {
		if (this.revokedAt != null) {
			throw new IllegalArgumentException("Cannot change revoked time");
		}
		if (revokedAt == null) {
			throw new IllegalArgumentException("Incorrect time");
		}
		if (this.createdAt.isAfter(revokedAt)) {
			throw new IllegalArgumentException("Cannot revoke session before creation");
		}
		if (this.lastUsedAt != null && this.lastUsedAt.isAfter(revokedAt)) {
			throw new IllegalArgumentException("Incorrect time");
		}
		this.revokedAt = revokedAt;
	}
	
	public void addLastUsedAt(Instant time) {
		if (time == null) {
			throw new IllegalArgumentException("Incorrect time");
		}
		if (this.revokedAt != null) {
			throw new IllegalArgumentException("Session already revoked");
		}
		if (this.createdAt.isAfter(time)) {
			throw new IllegalArgumentException("Incorrect time");
		}
		if (!this.expiresAt.isAfter(time)) {
			throw new IllegalArgumentException("Incorrect time");
		}
		if (this.lastUsedAt != null && !this.lastUsedAt.isBefore(time)) {
			throw new IllegalArgumentException("Incorrect time");
		}
		this.lastUsedAt = time;
	}
}
