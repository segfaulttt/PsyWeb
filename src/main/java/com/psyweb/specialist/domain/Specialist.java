package com.psyweb.specialist.domain;

import java.time.Duration;

import com.psyweb.common.persistence.DurationMinutesConverter;
import com.psyweb.specialist.exception.InvalidSpecialistDataException;
import com.psyweb.specialist.exception.InvalidSpecialistStateException;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
	
	@Column(name = "approval_status", nullable = false)
	@Enumerated(EnumType.STRING)
	private SpecialistStatus approvalStatus;
	
	@Column(name = "minimum_booking_notice_minutes", nullable = false)
	@Convert(converter = DurationMinutesConverter.class)
	private Duration minimumBookingNotice;
	
	@Column(name = "client_cancellation_notice_minutes", nullable = false)
	@Convert(converter = DurationMinutesConverter.class)
	private Duration clientCancellationNotice;
	
	protected Specialist() {}
	
	public Specialist(User user, String firstName, String lastName, Duration minimumBookingNotice, Duration clientCancellationNotice) {
		if (user == null) {
			throw new InvalidSpecialistDataException("User cannot be null");
		}
		if (user.getRole() != UserRole.SPECIALIST) {
			throw new InvalidSpecialistDataException("User must have SPECIALIST role");
		}
		validateFirstName(firstName);
		validateLastName(lastName);
		validateNotice(minimumBookingNotice, "Minimum booking notice");
		validateNotice(clientCancellationNotice, "Client cancellation notice");
		this.firstName = firstName;
		this.lastName = lastName;
		this.user = user;
		this.approvalStatus = SpecialistStatus.PENDING;
		this.minimumBookingNotice = minimumBookingNotice;
		this.clientCancellationNotice = clientCancellationNotice;
	}
	
	private static void validateFirstName(String firstName) {
		if (firstName == null || firstName.isBlank()) {
			throw new InvalidSpecialistDataException("First name cannot be blank");
		}
	}
	
	private static void validateLastName(String lastName) {
		if (lastName == null || lastName.isBlank()) {
			throw new InvalidSpecialistDataException("Last name cannot be blank");
		}
	}
	
	private static void validateNotice(Duration notice, String fieldName) {
		if (notice == null) {
			throw new InvalidSpecialistDataException(fieldName + " cannot be null");
		}
			
		if (notice.isNegative()) {
			throw new InvalidSpecialistDataException(fieldName + " cannot be negative");
		}
			
		if (notice.getSeconds() % 60 != 0 || notice.getNano() != 0) {
			throw new InvalidSpecialistDataException(fieldName + " must contain whole minutes");
		}
			
		if (notice.toMinutes() > Integer.MAX_VALUE) {
			throw new InvalidSpecialistDataException(fieldName + " exceeds supported range");
		}
	}
	
	public Long getId() {
	    return id;
	}
	
	public String getFirstName() {
		return this.firstName;
	}
	
	public String getLastName() {
		return this.lastName;
	}
	
	public Duration getMinimumBookingNotice() {
		return this.minimumBookingNotice;
	}
	public Duration getClientCancellationNotice() {
		return this.clientCancellationNotice;
	}
	
	public void changeFirstName(String newFirstName) {
		validateFirstName(newFirstName);
		this.firstName = newFirstName;
	}
	
	public void changeLastName(String newLastName) {
		validateLastName(newLastName);
		this.lastName = newLastName;
	}
	
	public void changeMinimumBookingNotice(Duration newNotice) {
		validateNotice(newNotice, "Minimum booking notice");
		this.minimumBookingNotice = newNotice;
	}
	
	public void changeClientCancellationNotice(Duration newNotice) {
		validateNotice(newNotice, "Client cancellation notice");
		this.clientCancellationNotice = newNotice;
	}
	
	public SpecialistStatus getApprovalStatus() {
		return this.approvalStatus;
	}
	
	public void approve() {
		if (this.approvalStatus == SpecialistStatus.PENDING) {
			this.approvalStatus = SpecialistStatus.APPROVED;
		} else {
			throw new InvalidSpecialistStateException("Cannot approve specialist with status " + approvalStatus);
		}
	}
	
	public void reject() {
		if (this.approvalStatus == SpecialistStatus.PENDING) {
			this.approvalStatus = SpecialistStatus.REJECTED;
		} else {
			throw new InvalidSpecialistStateException("Cannot reject specialist with status " + approvalStatus);
		}
	}
	
	public void resubmit() {
		if (this.approvalStatus == SpecialistStatus.REJECTED) {
			this.approvalStatus = SpecialistStatus.PENDING;
		} else {
			throw new InvalidSpecialistStateException("Cannot resubmit specialist with status " + approvalStatus);
		}
	}
	
	public void suspend() {
		if (this.approvalStatus == SpecialistStatus.APPROVED) {
			this.approvalStatus = SpecialistStatus.SUSPENDED;
		} else {
			throw new InvalidSpecialistStateException("Cannot suspend specialist with status " + approvalStatus);
		}
	}
	
	public void reinstate() {
		if (this.approvalStatus == SpecialistStatus.SUSPENDED) {
			this.approvalStatus = SpecialistStatus.APPROVED;
		} else {
			throw new InvalidSpecialistStateException("Cannot reinstate specialist with status " + approvalStatus);
		}
	}
}
