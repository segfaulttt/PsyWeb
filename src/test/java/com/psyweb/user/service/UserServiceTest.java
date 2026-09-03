package com.psyweb.user.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.exception.UserAccessForbiddenException;
import com.psyweb.user.exception.UserNotFoundException;
import com.psyweb.user.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
	
	private User user = new User("user@example.ru", "password", UserRole.CLIENT, UserStatus.ACTIVE);
	private User userBlocked = new User("userblocked@example.ru", "passwordsec", UserRole.CLIENT, UserStatus.BLOCKED);
	
	@Mock
	UserRepository repository;
	
	@InjectMocks
	UserService service;
	
	@Test
	public void shouldReturnUserByNormalizedEmail() {
		String email = "user@example.ru";
		when(repository.findByEmail("user@example.ru")).thenReturn(Optional.of(user));
		User result = service.findUserByEmail(email);
		
		assertEquals(user, result);
	}
	
	@Test
	public void shouldNormalizeMixedCaseEmailBeforeLookup() {
		String email = "uSEr@exaMplE.RU";
		when(repository.findByEmail("user@example.ru")).thenReturn(Optional.of(user));
		User result = service.findUserByEmail(email);
		
		assertEquals(user, result);
	}
	
	@Test
	public void shouldTrimEmailBeforeLookup() {
		String email = "      user@example.ru  ";
		when(repository.findByEmail("user@example.ru")).thenReturn(Optional.of(user));
		User result = service.findUserByEmail(email);
		
		assertEquals(user, result);
	}
	
	@Test
	public void shouldThrowExceptionWhenUserNotFoundByEmail() {
		String email = "null@example.ru";
		when(repository.findByEmail("null@example.ru")).thenReturn(Optional.empty());
		UserNotFoundException exception = assertThrows(UserNotFoundException.class,
				() -> service.findUserByEmail(email));
		
		assertEquals("USER_NOT_FOUND", exception.code());
		assertEquals("User not found", exception.getMessage());
	}
	
	@Test
	public void shouldRejectNullEmailWithoutCallingRepository() {
		InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
				() -> service.findUserByEmail(null));
		
		assertEquals("USER_INVALID_DATA", exception.code());
		assertEquals("Email cannot be blank", exception.getMessage());
		verify(repository, never()).findByEmail(any());
	}
	
	@Test
	public void shouldRejectBlankEmailWithoutCallingRepository() {
		InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
				() -> service.findUserByEmail("   "));
		
		assertEquals("USER_INVALID_DATA", exception.code());
		assertEquals("Email cannot be blank", exception.getMessage());
		verify(repository, never()).findByEmail(any());
	}
	
	@Test
	public void shouldRejectInvalidEmailWithoutCallingRepository() {
		InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
				() -> service.findUserByEmail("incorrect-email"));
		
		assertEquals("USER_INVALID_DATA", exception.code());
		assertEquals("Invalid email format", exception.getMessage());
		verify(repository, never()).findByEmail(any());
	}
	
	@Test
	public void shouldReturnActiveUserByIdWithoutStatusRestriction() {
		Long id = 1L;
		when(repository.findById(id)).thenReturn(Optional.of(user));
		
		User result = service.getUser(id);
		
		assertEquals(user, result);
	}
	
	@Test
	public void shouldReturnBlockedUserByIdWithoutStatusRestriction() {
		Long id = 11L;
		when(repository.findById(id)).thenReturn(Optional.of(userBlocked));
		
		User result = service.getUser(id);
		
		assertEquals(userBlocked, result);
	}
	
	@Test
	public void shouldThrowExceptionWhenUserNotFoundById() {
		Long id = 100L;
		when(repository.findById(id)).thenReturn(Optional.empty());
		
		UserNotFoundException exception = assertThrows(UserNotFoundException.class , () -> service.getUser(id));
		
		assertEquals("USER_NOT_FOUND", exception.code());
		assertEquals("User not found", exception.getMessage());
	}
	
	@Test
	public void shouldRejectNullUserIdWithoutCallingRepository() {
		InvalidUserDataException exception = assertThrows(InvalidUserDataException.class , () -> service.getUser(null));
		
		assertEquals("USER_INVALID_DATA", exception.code());
		assertEquals("User id cannot be null", exception.getMessage());
		verify(repository, never()).findById(any());
	}
	
	@Test
	public void shouldReturnActiveUserWhenActiveAccessRequired() {
		Long id = 1L;
		when(repository.findById(id)).thenReturn(Optional.of(user));
		
		User result = service.getActiveUser(id);
		
		assertEquals(user, result);
	}
	
	@Test
	public void shouldRejectBlockedUserWhenActiveAccessRequired() {
		Long id = 11L;
		when(repository.findById(id)).thenReturn(Optional.of(userBlocked));
		
		UserAccessForbiddenException exception = assertThrows(UserAccessForbiddenException.class , () -> service.getActiveUser(id));
		
		assertEquals("USER_ACCESS_FORBIDDEN", exception.code());
		assertEquals("User must have status 'ACTIVE'", exception.getMessage());
	}
	
}