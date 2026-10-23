package com.psyweb.auth.domain;

import java.time.Instant;
import java.util.UUID;

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
@Table(name = "refresh_tokens")
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id", nullable = false)
	private RefreshSession session;

	@Column(name = "token_hash", nullable = false, unique = true)
	private String tokenHash;

	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt;

	@Column(name = "consumed_at")
	private Instant consumedAt;

	protected RefreshToken() {
	}

	public RefreshToken(RefreshSession session, String tokenHash, Instant issuedAt) {
		if (session == null || session.getRevokedAt() != null) {
			throw new IllegalArgumentException("invalid session");
		}
		if (tokenHash == null || tokenHash.length() != 64) {
			throw new IllegalArgumentException("Incorrect token hash");
		}
		if (issuedAt == null || issuedAt.isBefore(session.getCreatedAt()) || !session.getExpiresAt().isAfter(issuedAt)) {
			throw new IllegalArgumentException("Incorrect time");
		}
		this.session = session;
		this.issuedAt = issuedAt;
		this.tokenHash = tokenHash;
	}

	public UUID getId() {
		return this.id;
	}

	public UUID getSessionId() {
		return this.session.getId();
	}

	public String getTokenHash() {
		return this.tokenHash;
	}

	public Instant getIssuedAt() {
		return this.issuedAt;
	}

	public Instant getConsumedAt() {
		return this.consumedAt;
	}

	public void consumeToken(Instant time) {
		if (time == null) {
			throw new IllegalArgumentException("Incorrect time");
		}
		if (this.consumedAt != null) {
			throw new IllegalArgumentException("Token already consumed");
		}
		if (this.issuedAt.isAfter(time)) {
			throw new IllegalArgumentException("Incorrect time");
		}
		if (session.getRevokedAt() != null || !session.getExpiresAt().isAfter(time)) {
			throw new IllegalArgumentException("Incorrect session state");
		}

		this.consumedAt = time;
	}
}
