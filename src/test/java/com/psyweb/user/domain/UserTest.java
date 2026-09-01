package com.psyweb.user.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {
	
	
	@Test
	public void shouldChangePassword() {
		User user = new User("user@example.com", "password", UserRole.CLIENT, UserStatus.ACTIVE);
		
		user.changePasswordHash("newPassword");
		
		assertEquals("newPassword", user.getPasswordHash());
	}
	
	@Test
	public void shouldRejectNullPasswordHash() {
		User user = new User("user@example.com", "password", UserRole.CLIENT, UserStatus.ACTIVE);
		
		Exception excep = assertThrows(IllegalArgumentException.class, () -> user.changePasswordHash(null));
		
		assertEquals("Password hash cannot be blank", excep.getMessage());
		assertEquals("password", user.getPasswordHash());
	}
	
	@Test
	public void shouldRejectBlankPasswordHash() {
		User user = new User("user@example.com", "password", UserRole.CLIENT, UserStatus.ACTIVE);
		
		Exception excep = assertThrows(IllegalArgumentException.class, () -> user.changePasswordHash(""));
		
		assertEquals("Password hash cannot be blank", excep.getMessage());
		assertEquals("password", user.getPasswordHash());
		
		excep = assertThrows(IllegalArgumentException.class, () -> user.changePasswordHash("    "));
		
		assertEquals("Password hash cannot be blank", excep.getMessage());
		assertEquals("password", user.getPasswordHash());
	}
	
	@Test
	public void shouldRejectNullPasswordHashOnCreation() {
		Exception excep = assertThrows(IllegalArgumentException.class, 
				() -> new User("user@example.com", null, UserRole.CLIENT, UserStatus.ACTIVE));
		assertEquals("Password hash cannot be blank", excep.getMessage());
	}
	
	@Test
	public void shouldRejectBlankPasswordHashOnCreation() {
		Exception excep = assertThrows(IllegalArgumentException.class, 
				() -> new User("user@example.com", "   ", UserRole.CLIENT, UserStatus.ACTIVE));
		assertEquals("Password hash cannot be blank", excep.getMessage());
	}
}

