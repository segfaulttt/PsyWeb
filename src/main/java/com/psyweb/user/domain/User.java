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
	
	protected User() {}
	
	public User(String email, String passwordHash, UserRole role, UserStatus status) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Email cannot be blank");
		}
		if (passwordHash == null || passwordHash.isBlank()) {
			throw new IllegalArgumentException("Password cannot be blank");
		}
		if (role == null) {
			throw new IllegalArgumentException("");
		}
		if (status == null) {
			throw new IllegalArgumentException("");
		}
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.status = status;
	}
	
	public Long getId() {
		return this.id;
	}
	
	public String getEmail() {
		return this.email;
	}
	
	public void changeEmail(String newEmail) {
		if (newEmail == null || newEmail.isBlank()) {
	        throw new IllegalArgumentException("Email cannot be blank");
	    }
		this.email = newEmail;
	}
	
	public String getPasswordHash() {
		return this.passwordHash;
	}
	
	public void changePasswordHash(String newPasswordHash) {
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
