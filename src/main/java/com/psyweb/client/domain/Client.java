package com.psyweb.client.domain;

import com.psyweb.client.exception.InvalidClientDataException;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

@Entity
public class Client {
	@Id
	private Long id;

	@MapsId
	@OneToOne
	private User user;

	@Column(name = "first_name", nullable = false)
	private String firstName;

	@Column(name = "last_name", nullable = false)
	private String lastName;
	
	protected Client() {}

	public Client(User user, String firstName, String lastName) {
		if (user == null) {
			throw new InvalidClientDataException("User cannot be null");
		}
		if (!user.getRole().equals(UserRole.CLIENT)) {
			throw new InvalidClientDataException("User must have CLIENT role");
		}
		validateFirstName(firstName);
		validateLastName(lastName);
		this.user = user;
		this.firstName = firstName;
		this.lastName = lastName;
	}
	
	private static void validateFirstName(String firstName) {
		if (firstName == null || firstName.isBlank()) {
			throw new InvalidClientDataException("First name cannot be blank");
		}
	}
	
	private static void validateLastName(String lastName) {
		if (lastName == null || lastName.isBlank()) {
			throw new InvalidClientDataException("Last name cannot be blank");
		}
	}
	
	public Long getId() {
		return this.id;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public String getFLastName() {
		return this.lastName;
	}
	
	public void changeFirstName(String newFirstName) {
		validateFirstName(newFirstName);
		this.firstName = newFirstName;
	}
	
	public void changeLastName(String newLastName) {
		validateLastName(newLastName);
		this.lastName = newLastName;
	}
}
