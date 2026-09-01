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
	
	@Test
	public void constructorRejectsNullEmail() {
		Exception excep = assertThrows(IllegalArgumentException.class,
				() -> new User(null, "password", UserRole.CLIENT, UserStatus.ACTIVE));
		assertEquals("Email cannot be blank", excep.getMessage());
	}
	
	@Test
	public void constructorRejectsBlankEmail() {
		Exception excep = assertThrows(IllegalArgumentException.class,
				() -> new User("   ", "password", UserRole.CLIENT, UserStatus.ACTIVE));
		assertEquals("Email cannot be blank", excep.getMessage());
	}
	
	@Test
	public void constructorRejectsInvalidEmailFormat() {
		String email = "iNcorreCt@";
		Exception excep = assertThrows(IllegalArgumentException.class,
				() -> new User(email, "password", UserRole.CLIENT, UserStatus.ACTIVE));
		assertEquals("Incorrect email", excep.getMessage());
		
		String emailSec = "@incorrect";
		excep = assertThrows(IllegalArgumentException.class,
				() -> new User(emailSec, "password", UserRole.CLIENT, UserStatus.ACTIVE));
		assertEquals("Incorrect email", excep.getMessage());
		
		String emailTh = "incorrect@@example.ru";
		excep = assertThrows(IllegalArgumentException.class,
				() -> new User(emailTh, "password", UserRole.CLIENT, UserStatus.ACTIVE));
		assertEquals("Incorrect email", excep.getMessage());
	}
	
	@Test
	public void constructorNormalizesEmailToLowercase() {
		String email = "iNcorReCt@example.ru";
		User user = new User(email, "password", UserRole.CLIENT, UserStatus.ACTIVE);
		assertEquals("incorrect@example.ru", user.getEmail());
	}
	
	@Test
	public void constructorTrimsSurroundingWhitespace() {
		String email = "  incorrect@example.ru    ";
		User user = new User(email, "password", UserRole.CLIENT, UserStatus.ACTIVE);
		assertEquals("incorrect@example.ru", user.getEmail());
	}
	
	@Test
	public void changeEmailRejectsNullEmail() {
		String email = "correct@example.ru";
		User user = new User(email, "password", UserRole.CLIENT, UserStatus.ACTIVE);
		
		Exception excep = assertThrows(IllegalArgumentException.class, () -> user.changeEmail(null));
		assertEquals("Email cannot be blank", excep.getMessage());
		assertEquals("correct@example.ru", user.getEmail());
	}
	
	@Test
	public void changeEmailRejectsBlankEmail() {
		String email = "correct@example.ru";
		User user = new User(email, "password", UserRole.CLIENT, UserStatus.ACTIVE);
		
		Exception excep = assertThrows(IllegalArgumentException.class, () -> user.changeEmail("    "));
		assertEquals("Email cannot be blank", excep.getMessage());
		assertEquals("correct@example.ru", user.getEmail());
	}
	
	@Test
	public void changeEmailRejectsInvalidEmailFormat() {
		String email = "correct@example.ru";
		User user = new User(email, "password", UserRole.CLIENT, UserStatus.ACTIVE);
		String emailInc = "  in@corRect@example.ru  ";
		
		Exception excep = assertThrows(IllegalArgumentException.class, () -> user.changeEmail(emailInc));
		assertEquals("Incorrect email", excep.getMessage());
		assertEquals("correct@example.ru", user.getEmail());
	}
	
	@Test
	public void changeEmailNormalizesEmailToLowercase() {
		String email = "correct@example.ru";
		User user = new User(email, "password", UserRole.CLIENT, UserStatus.ACTIVE);
		String emailInc = "INCORRECT@example.RU";
		
		user.changeEmail(emailInc);
		assertEquals("incorrect@example.ru", user.getEmail());
	}
	
	@Test
	public void changeEmailTrimsSurroundingWhitespace() {
		String email = "correct@example.ru";
		User user = new User(email, "password", UserRole.CLIENT, UserStatus.ACTIVE);
		String emailInc = "   incorrect@example.ru  ";
		
		user.changeEmail(emailInc);
		assertEquals("incorrect@example.ru", user.getEmail());
	}
	
	@Test
	public void constructorNormalizesMixedCaseEmailWithSurroundingWhitespace() {
		String email = "   iNcorReCt@example.ru     ";
		User user = new User(email, "password", UserRole.CLIENT, UserStatus.ACTIVE);
		assertEquals("incorrect@example.ru", user.getEmail());
	}
}

