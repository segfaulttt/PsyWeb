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
import com.psyweb.user.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
	
	private User user = new User("user@example.ru", "password", UserRole.CLIENT, UserStatus.ACTIVE);
	
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
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> service.findUserByEmail(email));
		
		assertEquals("User not found", exception.getMessage());
	}
	
	@Test
	public void shouldRejectNullEmailWithoutCallingRepository() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> service.findUserByEmail(null));
		
		assertEquals("Email cannot be blank", exception.getMessage());
		verify(repository, never()).findByEmail(any());
	}
}