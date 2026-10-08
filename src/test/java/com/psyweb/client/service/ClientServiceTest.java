package com.psyweb.client.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.psyweb.client.domain.Client;
import com.psyweb.client.exception.ClientNotFoundException;
import com.psyweb.client.exception.InvalidClientDataException;
import com.psyweb.client.repository.ClientRepository;
import com.psyweb.user.service.UserService;

class ClientServiceTest {

	private ClientRepository clientRepository;
	private UserService userService;
	private ClientService clientService;

	@BeforeEach
	void setUp() {
		clientRepository = mock(ClientRepository.class);
		userService = mock(UserService.class);
		clientService = new ClientService(clientRepository, userService);
	}

	@Test
	void shouldReturnClientById() {
		Long clientId = 1L;
		Client client = mock(Client.class);

		when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

		Client result = clientService.getClient(clientId);

		assertSame(client, result);
		verify(clientRepository).findById(clientId);
	}

	@Test
	void shouldRejectNullClientId() {
		assertThrows(InvalidClientDataException.class, () -> clientService.getClient(null));
	}

	@Test
	void shouldThrowWhenClientDoesNotExist() {
		Long clientId = 1L;

		when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

		assertThrows(ClientNotFoundException.class, () -> clientService.getClient(clientId));
	}

	@Test
	void shouldReturnActiveClient() {
		Long clientId = 1L;
		Client client = mock(Client.class);

		when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

		Client result = clientService.getActiveClient(clientId);

		assertSame(client, result);
		verify(userService).getActiveUser(clientId);
		verify(clientRepository).findById(clientId);
	}
}