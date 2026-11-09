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
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "token_hash", nullable = false, unique = true)
	private String tokenHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "consumed_at")
	private Instant consumedAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected PasswordResetToken() {
	}

	public PasswordResetToken(User user, String tokenHash, Instant createdAt, Instant expiresAt) {
		if (user == null) {
			throw new IllegalArgumentException("User cannot be null");
		}

		if (tokenHash == null || !tokenHash.matches("[0-9a-f]{64}")) {
			throw new IllegalArgumentException("Invalid token hash");
		}

		if (createdAt == null || expiresAt == null || !createdAt.isBefore(expiresAt)) {
			throw new IllegalArgumentException("Invalid token lifetime");
		}

		this.user = user;
		this.tokenHash = tokenHash;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public UUID getId() {
		return id;
	}

	public Long getUserId() {
		return user.getId();
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getConsumedAt() {
		return consumedAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public boolean isUsableAt(Instant now) {
		if (now == null) {
			throw new IllegalArgumentException("Current time cannot be null");
		}

		return consumedAt == null && revokedAt == null && !now.isBefore(createdAt) && now.isBefore(expiresAt);
	}

	public void consume(Instant now) {
		if (!isUsableAt(now)) {
			throw new IllegalStateException("Password reset token is not usable");
		}

		consumedAt = now;
	}

	public void revoke(Instant now) {
		if (now == null || now.isBefore(createdAt)) {
			throw new IllegalArgumentException("Invalid revocation time");
		}

		if (consumedAt != null || revokedAt != null) {
			return;
		}

		revokedAt = now;
	}
}