
package com.psyweb.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.psyweb.auth.exception.InvalidPasswordException;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.exception.UserEmailAlreadyExistsException;
import com.psyweb.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private UserService userService;

	@InjectMocks
	private RegistrationService registrationService;

	private static final String EMAIL = "testemail@example.com";
	private static final String PASSWORD = "ValidPassword12345!";
	private static final String HASH = "encoded-password-hash";

	@Test
	void shouldRegisterClientWithEncodedPassword() {
		User user = new User(EMAIL, HASH, UserRole.CLIENT, UserStatus.ACTIVE);

		when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
		when(userService.createUser(EMAIL, HASH, UserRole.CLIENT)).thenReturn(user);

		User result = registrationService.register(EMAIL, PASSWORD, UserRole.CLIENT);

		assertSame(user, result);
		verify(passwordEncoder).encode(PASSWORD);
		verify(userService).createUser(EMAIL, HASH, UserRole.CLIENT);
	}

	@Test
	void shouldRegisterSpecialistWithEncodedPassword() {
		User user = new User(EMAIL, HASH, UserRole.SPECIALIST, UserStatus.ACTIVE);

		when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
		when(userService.createUser(EMAIL, HASH, UserRole.SPECIALIST)).thenReturn(user);

		User result = registrationService.register(EMAIL, PASSWORD, UserRole.SPECIALIST);

		assertSame(user, result);
		verify(passwordEncoder).encode(PASSWORD);
		verify(userService).createUser(EMAIL, HASH, UserRole.SPECIALIST);
	}

	@Test
	void shouldNormalizeEmailBeforeCreatingUser() {
		String email = "  TestEmail@Example.COM  ";

		User user = new User(EMAIL, HASH, UserRole.CLIENT, UserStatus.ACTIVE);

		when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
		when(userService.createUser(EMAIL, HASH, UserRole.CLIENT)).thenReturn(user);

		User result = registrationService.register(email, PASSWORD, UserRole.CLIENT);

		assertSame(user, result);
		verify(userService).createUser(EMAIL, HASH, UserRole.CLIENT);
	}

	@Test
	void shouldRejectInvalidPasswordWithoutEncoding() {
		assertThrows(InvalidPasswordException.class,
				() -> registrationService.register(EMAIL, "short", UserRole.CLIENT));

		verifyNoInteractions(passwordEncoder, userService);
	}

	@Test
	void shouldRejectAdminRoleWithoutEncoding() {
		assertThrows(InvalidUserDataException.class,
				() -> registrationService.register(EMAIL, PASSWORD, UserRole.ADMIN));

		verifyNoInteractions(passwordEncoder, userService);
	}

	@Test
	void shouldRejectNullRoleWithoutEncoding() {
		assertThrows(InvalidUserDataException.class, () -> registrationService.register(EMAIL, PASSWORD, null));

		verifyNoInteractions(passwordEncoder, userService);
	}

	@Test
	void shouldPropagateEmailAlreadyExistsException() {
		when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);

		when(userService.createUser(EMAIL, HASH, UserRole.CLIENT))
				.thenThrow(new UserEmailAlreadyExistsException("User with this email already exists"));

		assertThrows(UserEmailAlreadyExistsException.class,
				() -> registrationService.register(EMAIL, PASSWORD, UserRole.CLIENT));

		verify(passwordEncoder).encode(PASSWORD);
		verify(userService).createUser(EMAIL, HASH, UserRole.CLIENT);
	}
}
