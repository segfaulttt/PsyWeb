package com.psyweb.specialist.domain;

import com.psyweb.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "specialists")
public class Specialist {	
	@Id
	private Long id;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;
	
	@Column(name = "first_name", nullable = false)
	private String firstName;
	
	@Column(name = "last_name", nullable = false)
	private String lastName;
	
	@Column(name = "bio")
	private String bio;
	
	@Column(name = "approval_status", nullable = false)
	@Enumerated(EnumType.STRING)
	private SpecialistStatus approvalStatus;
	
	protected Specialist() {}
	
	public Specialist(User user, String firstName, String lastName, String bio) {
		if (user == null) {
			throw new IllegalArgumentException("user cannot be blank");
		}
		if (firstName == null || firstName.isBlank()) {
			throw new IllegalArgumentException("Firstname cannot be blank");
		}
		if (lastName == null || lastName.isBlank()) {
			throw new IllegalArgumentException("Lastname cannot be blank");
		}
		if (bio == null || bio.isBlank()) {
			throw new IllegalArgumentException("Bio cannot be blank");
		}
		this.firstName = firstName;
		this.lastName = lastName;
		this.bio = bio;
		this.user = user;
		this.approvalStatus = SpecialistStatus.PENDING;
	}
	
	public Long getId() {
	    return user.getId();
	}
	
	public String getFirstName() {
		return this.firstName;
	}
	
	public void setFirstName(String newFirstName) {
		this.firstName = newFirstName;
	}
	
	public String getLastName() {
		return this.lastName;
	}
	
	public void setLastName(String newLastName) {
		this.lastName = newLastName;
	}
	
	public String getBio() {
		return this.bio;
	}
	
	public void setBio(String newBio) {
		this.bio = newBio;
	}
	
	public SpecialistStatus getApprovalStatus() {
		return this.approvalStatus;
	}
	
	void approve() {
		this.approvalStatus = SpecialistStatus.APPROVED;
	}
	
	void reject() {
		this.approvalStatus = SpecialistStatus.REJECTED;
	}
}
