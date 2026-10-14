package com.psyweb.auth.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.psyweb.auth.exception.InvalidPasswordException;

public class PasswordPolicyTest {
	
	
	@Test
	public void shouldRejectNullPassword() {
		InvalidPasswordException exception = assertThrows(InvalidPasswordException.class,
				() -> PasswordPolicy.validatePassword(null));
		
		assertEquals("PASSWORD_INVALID", exception.code());
		assertEquals("Password cannot be null", exception.getMessage());
	}
	
	@Test
	public void shouldRejectEmptyPassword() {
		InvalidPasswordException exception = assertThrows(InvalidPasswordException.class,
				() -> PasswordPolicy.validatePassword(""));
		
		assertEquals("PASSWORD_INVALID", exception.code());
		assertEquals("Password must contain at least 15 characters", exception.getMessage());
	}
	
	@Test
	public void shouldRejectPasswordShorterThan15Characters() {
		String password = "a".repeat(14);
		InvalidPasswordException exception = assertThrows(InvalidPasswordException.class,
				() -> PasswordPolicy.validatePassword(password));
		
		assertEquals("PASSWORD_INVALID", exception.code());
		assertEquals("Password must contain at least 15 characters", exception.getMessage());
	}
	
	@Test
	public void shouldAcceptPasswordWith15Characters() {
		String password = "a".repeat(15);
		assertDoesNotThrow(() -> PasswordPolicy.validatePassword(password));
	}
	
	@Test
	public void shouldAcceptPasswordWith128Characters() {
		String password = "a".repeat(128);
		assertDoesNotThrow(() -> PasswordPolicy.validatePassword(password));
	}
	
	@Test
	public void shouldRejectPasswordLongerThan128Characters() {
		String password = "a".repeat(129);
		InvalidPasswordException exception = assertThrows(InvalidPasswordException.class,
				() -> PasswordPolicy.validatePassword(password));
		
		assertEquals("PASSWORD_INVALID", exception.code());
		assertEquals("Password must not exceed 128 characters", exception.getMessage());
	}
	
	@Test
	public void shouldCountUnicodeCodePointsCorrectly() {
		String password = "a".repeat(14) + "\uD83D\uDE00";
		assertDoesNotThrow(() -> PasswordPolicy.validatePassword(password));
	}
}
