package com.psyweb.client.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.psyweb.client.exception.InvalidClientDataException;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

public class ClientTest {

	private final User clientUser = new User("client@example.com", "password", UserRole.CLIENT, UserStatus.ACTIVE);

	@Test
	public void shouldCreateClient() {
		Client client = new Client(clientUser, "Anna", "Smith");

		assertEquals("Anna", client.getFirstName());
		assertEquals("Smith", client.getLastName());
	}

	@Test
	public void shouldRejectNullUser() {
		assertThrows(InvalidClientDataException.class, () -> new Client(null, "Anna", "Smith"));
	}

	@Test
	public void shouldRejectUserWithoutClientRole() {
		User specialistUser = new User("specialist@example.com", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);

		assertThrows(InvalidClientDataException.class, () -> new Client(specialistUser, "Anna", "Smith"));
	}

	@Test
	public void shouldRejectNullFirstName() {
		assertThrows(InvalidClientDataException.class, () -> new Client(clientUser, null, "Smith"));
	}

	@Test
	public void shouldRejectBlankFirstName() {
		assertThrows(InvalidClientDataException.class, () -> new Client(clientUser, " ", "Smith"));
	}

	@Test
	public void shouldRejectNullLastName() {
		assertThrows(InvalidClientDataException.class, () -> new Client(clientUser, "Anna", null));
	}

	@Test
	public void shouldRejectBlankLastName() {
		assertThrows(InvalidClientDataException.class, () -> new Client(clientUser, "Anna", " "));
	}

	@Test
	public void shouldChangeFirstName() {
		Client client = new Client(clientUser, "Anna", "Smith");

		client.changeFirstName("Maria");

		assertEquals("Maria", client.getFirstName());
	}

	@Test
	public void shouldRejectBlankFirstNameWhenChanging() {
		Client client = new Client(clientUser, "Anna", "Smith");

		assertThrows(InvalidClientDataException.class, () -> client.changeFirstName(" "));
	}

	@Test
	public void shouldChangeLastName() {
		Client client = new Client(clientUser, "Anna", "Smith");

		client.changeLastName("Brown");

		assertEquals("Brown", client.getLastName());
	}

	@Test
	public void shouldRejectBlankLastNameWhenChanging() {
		Client client = new Client(clientUser, "Anna", "Smith");

		assertThrows(InvalidClientDataException.class, () -> client.changeLastName(" "));
	}
}