package com.psyweb.auth.service;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.psyweb.auth.validation.PasswordPolicy;
import com.psyweb.client.domain.Client;
import com.psyweb.client.service.ClientService;
import com.psyweb.common.config.SpecialistDefaultsProperties;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.service.SpecialistService;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.exception.UserEmailAlreadyExistsException;
import com.psyweb.user.service.UserService;

import jakarta.transaction.Transactional;

@Service
public class RegistrationService {
	private final PasswordEncoder passwordEncoder;
	private final UserService userService;
	private final ClientService clientService;
	private final SpecialistService specialistService;
	private final SpecialistDefaultsProperties specialistDefaults;

	public RegistrationService(PasswordEncoder passwordEncoder, UserService userService, ClientService clientService,
			SpecialistService specialistService, SpecialistDefaultsProperties specialistDefaults) {
		this.passwordEncoder = passwordEncoder;
		this.userService = userService;
		this.clientService = clientService;
		this.specialistService = specialistService;
		this.specialistDefaults = specialistDefaults;
	}

	@Transactional
	public Client registerClient(String email, String rawPassword, String firstName, String lastName) {
		String normalizedEmail = User.normalizeEmail(email);
		PasswordPolicy.validatePassword(rawPassword);
		String encodedPassword = passwordEncoder.encode(rawPassword);
		try {
			User user = userService.createUser(normalizedEmail, encodedPassword, UserRole.CLIENT);
			return clientService.createClient(user, firstName, lastName);
		} catch (DataIntegrityViolationException e) {
			if (isEmailUniqueViolation(e)) {
				throw new UserEmailAlreadyExistsException("Email already exists", e);
			}
			throw e;
		}
	}

	@Transactional
	public Specialist registerSpecialist(String email, String rawPassword, String firstName,
			String lastName) {
		String normalizedEmail = User.normalizeEmail(email);
		PasswordPolicy.validatePassword(rawPassword);
		String encodedPassword = passwordEncoder.encode(rawPassword);
		try {
			User user = userService.createUser(normalizedEmail, encodedPassword, UserRole.SPECIALIST);
			return specialistService.createSpecialist(user, firstName, lastName,
					specialistDefaults.minimumBookingNotice(), specialistDefaults.clientCancellationNotice());
		} catch (DataIntegrityViolationException e) {
			if (isEmailUniqueViolation(e)) {
				throw new UserEmailAlreadyExistsException("Email already exists", e);
			}
			throw e;
		}
	}

	private boolean isEmailUniqueViolation(DataIntegrityViolationException exception) {
		Throwable cause = exception;

		while (cause != null) {
			if (cause instanceof ConstraintViolationException constraintException) {
				return "23505".equals(constraintException.getSQLState())
						&& "users_email_key".equals(constraintException.getConstraintName());
			}
			cause = cause.getCause();
		}

		return false;
	}
}
