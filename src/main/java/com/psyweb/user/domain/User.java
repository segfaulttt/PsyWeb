package com.psyweb.user.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "email", nullable = false, unique = true)
	private String email;
	
	@Column(name = "password_hash", nullable = false)
	private String passwordHash;
	
	@Column(name = "role", nullable = false)
	@Enumerated(EnumType.STRING)
	private UserRole role;
	
	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private UserStatus status;
	
	@Column(name = "created_at", nullable = false, updatable = false)
	@CreationTimestamp
	private LocalDateTime createdAt;
	
	public Long getId() {
		return this.id;
	}
	
	public String getEmail() {
		return this.email;
	}
	
	public void setEmail(String newEmail) {
		this.email = newEmail;
	}
	
	public String getPasswordHash() {
		return this.passwordHash;
	}
	
	public void changePassword(String newPasswordHash) {
		this.passwordHash = newPasswordHash;
	}
	
	public UserRole getRole() {
		return this.role;
	}
	
	public UserStatus getStatus() {
		return this.status;
	}
	
	public void block() {
		this.status = UserStatus.BLOCKED;
	}
	
	public void activate() {
		this.status = UserStatus.ACTIVE;
	}
	
	public LocalDateTime getCreatedAt() {
		return this.createdAt;
	}
}
