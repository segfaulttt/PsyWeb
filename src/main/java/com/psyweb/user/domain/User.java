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
		email = normalizationEmail(email);
		validateEmail(email);
		validatePasswordHash(passwordHash);
		
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
	
	private static void validatePasswordHash(String passwordHash) {
		if (passwordHash == null || passwordHash.isBlank()) {
			throw new IllegalArgumentException("Password hash cannot be blank");
		}
	}
	
	private static void validateEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Email cannot be blank");
		}
		int idxF = email.indexOf('@');
		int idxL = email.lastIndexOf('@');
		
		if (idxF == -1 || idxF != idxL || idxF == 0 || idxF == email.length() - 1) {
			throw new IllegalArgumentException("Incorrect email");
		}
	}
	
	private static String normalizationEmail(String email) {
		if (email == null) {
			throw new IllegalArgumentException("Email cannot be blank");
		}
		return email.trim().toLowerCase();
	}
	
	public Long getId() {
		return this.id;
	}
	
	public String getEmail() {
		return this.email;
	}
	
	public void changeEmail(String newEmail) {
		newEmail = normalizationEmail(newEmail);
		validateEmail(newEmail);
		this.email = newEmail;
	}
	
	public String getPasswordHash() {
		return this.passwordHash;
	}
	
	public void changePasswordHash(String newPasswordHash) {
		validatePasswordHash(newPasswordHash);
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
