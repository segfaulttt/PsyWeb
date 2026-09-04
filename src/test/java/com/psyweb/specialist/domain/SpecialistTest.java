package com.psyweb.specialist.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.psyweb.specialist.exception.InvalidSpecialistDataException;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

public class SpecialistTest {
	private final User user = new User("userfirst@example.ru", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);
	
	@Test
	public void shouldRejectNullUserOnCreation() {
		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(null, "First", "Last", "bio"));
		
		assertEquals("SPECIALIST_INVALID_DATA", exception.code());
		assertEquals("User cannot be blank", exception.getMessage());
	}
	
	@Test
	public void shouldRejectBlankFirstNameOnCreation() {
		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, null, "Last", "bio"));
		
		assertEquals("SPECIALIST_INVALID_DATA", exception.code());
		assertEquals("Firstname cannot be blank", exception.getMessage());
	}
	
	@Test
	public void shouldRejectBlankLastNameOnCreation() {
		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, "First", null, "bio"));
		
		assertEquals("SPECIALIST_INVALID_DATA", exception.code());
		assertEquals("Lastname cannot be blank", exception.getMessage());
	}
	
	@Test
	public void shouldRejectBlankBioOnCreation() {
		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, "First", "Last", null));
		
		assertEquals("SPECIALIST_INVALID_DATA", exception.code());
		assertEquals("Bio cannot be blank", exception.getMessage());
	}
}