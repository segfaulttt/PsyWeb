package com.psyweb.user.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.exception.ConcurrentPasswordChangeException;
import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.exception.InvalidUserStateException;
import com.psyweb.user.exception.UserAccessForbiddenException;
import com.psyweb.user.exception.UserEmailAlreadyExistsException;
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

		UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> service.getUser(id));

		assertEquals("USER_NOT_FOUND", exception.code());
		assertEquals("User not found", exception.getMessage());
	}

	@Test
	public void shouldRejectNullUserIdWithoutCallingRepository() {
		InvalidUserDataException exception = assertThrows(InvalidUserDataException.class, () -> service.getUser(null));

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

		UserAccessForbiddenException exception = assertThrows(UserAccessForbiddenException.class,
				() -> service.getActiveUser(id));

		assertEquals("USER_ACCESS_FORBIDDEN", exception.code());
		assertEquals("User must have status 'ACTIVE'", exception.getMessage());
	}

	@Test
	void shouldCreateClientWithActiveStatus() {
		String email = "client@example.com";
		String hash = "encoded-password-hash";

		when(repository.existsByEmail(email)).thenReturn(false);
		when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = service.createUser(email, hash, UserRole.CLIENT);

		assertEquals(email, result.getEmail());
		assertEquals(hash, result.getPasswordHash());
		assertEquals(UserRole.CLIENT, result.getRole());
		assertEquals(UserStatus.ACTIVE, result.getStatus());
		assertNull(result.getEmailVerifiedAt());

		verify(repository).save(any(User.class));
	}

	@Test
	void shouldCreateSpecialistWithActiveStatus() {
		String email = "specialist@example.com";
		String hash = "encoded-password-hash";

		when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = service.createUser(email, hash, UserRole.SPECIALIST);

		assertEquals(UserRole.SPECIALIST, result.getRole());
		assertEquals(UserStatus.ACTIVE, result.getStatus());
		assertNull(result.getEmailVerifiedAt());

		verify(repository).save(any(User.class));
	}

	@Test
	void shouldNormalizeEmailWhenCreatingUser() {
		String email = "  TestEmail@Example.COM  ";
		String normalizedEmail = "testemail@example.com";
		String hash = "encoded-password-hash";

		when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = service.createUser(email, hash, UserRole.CLIENT);

		assertEquals(normalizedEmail, result.getEmail());

		verify(repository).existsByEmail(normalizedEmail);
		verify(repository).save(any(User.class));
	}

	@Test
	void shouldRejectDuplicateEmailWhenCreatingUser() {
		String email = "client@example.com";

		when(repository.existsByEmail(email)).thenReturn(true);

		UserEmailAlreadyExistsException exception = assertThrows(UserEmailAlreadyExistsException.class,
				() -> service.createUser(email, "encoded-password-hash", UserRole.CLIENT));

		assertEquals("User with this email already exists", exception.getMessage());

		verify(repository).existsByEmail(email);
		verify(repository, never()).save(any(User.class));
	}

	@Test
	void shouldRejectAdminRoleWhenCreatingUser() {
		assertThrows(InvalidUserDataException.class,
				() -> service.createUser("admin@example.com", "encoded-password-hash", UserRole.ADMIN));

		verify(repository, never()).save(any(User.class));
	}

	@Test
	void shouldRejectNullRoleWhenCreatingUser() {
		assertThrows(InvalidUserDataException.class,
				() -> service.createUser("client@example.com", "encoded-password-hash", null));

		verify(repository, never()).save(any(User.class));
	}

	@Test
	void shouldPersistProvidedPasswordHash() {
		String email = "client@example.com";
		String hash = "encoded-password-hash";

		when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = service.createUser(email, hash, UserRole.CLIENT);

		assertEquals(hash, result.getPasswordHash());

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

		verify(repository).save(captor.capture());

		assertEquals(hash, captor.getValue().getPasswordHash());
	}

	@Test
	void shouldChangePasswordWhenCurrentHashMatches() {
		Long userId = 1L;
		String oldHash = "old-hash";
		String newHash = "new-hash";

		User user = new User("client-password@example.com", oldHash, UserRole.CLIENT, UserStatus.ACTIVE);

		when(repository.findForUpdateById(userId)).thenReturn(Optional.of(user));

		User result = service.changePassword(userId, oldHash, newHash);

		assertSame(user, result);
		assertEquals(newHash, result.getPasswordHash());

		verify(repository).findForUpdateById(userId);
	}

	@Test
	void shouldRejectPasswordChangeWhenUserNotFound() {
		Long userId = 100L;

		when(repository.findForUpdateById(userId)).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class, () -> service.changePassword(userId, "old-hash", "new-hash"));

		verify(repository).findForUpdateById(userId);
	}

	@Test
	void shouldRejectPasswordChangeForBlockedUser() {
		Long userId = 1L;
		String oldHash = "old-hash";

		User blockedUser = new User("blocked-password@example.com", oldHash, UserRole.CLIENT, UserStatus.BLOCKED);

		when(repository.findForUpdateById(userId)).thenReturn(Optional.of(blockedUser));

		assertThrows(InvalidUserStateException.class, () -> service.changePassword(userId, oldHash, "new-hash"));

		assertEquals(oldHash, blockedUser.getPasswordHash());

		verify(repository).findForUpdateById(userId);
	}

	@Test
	void shouldRejectConcurrentPasswordChange() {
		Long userId = 1L;
		String previousHash = "old-hash";
		String actualHash = "already-updated-hash";

		User user = new User("concurrent-password@example.com", actualHash, UserRole.CLIENT, UserStatus.ACTIVE);

		when(repository.findForUpdateById(userId)).thenReturn(Optional.of(user));

		ConcurrentPasswordChangeException exception = assertThrows(ConcurrentPasswordChangeException.class,
				() -> service.changePassword(userId, previousHash, "new-hash"));

		assertEquals("PASSWORD_CONFLICT", exception.code());
		assertEquals(actualHash, user.getPasswordHash());

		verify(repository).findForUpdateById(userId);
	}

	@Test
	void shouldRejectBlankNewPasswordHash() {
		Long userId = 1L;
		String oldHash = "old-hash";

		User user = new User("blank-hash@example.com", oldHash, UserRole.CLIENT, UserStatus.ACTIVE);

		when(repository.findForUpdateById(userId)).thenReturn(Optional.of(user));

		assertThrows(InvalidUserDataException.class, () -> service.changePassword(userId, oldHash, "   "));

		assertEquals(oldHash, user.getPasswordHash());
	}

}