package com.psyweb.client.service;

import org.springframework.stereotype.Service;

import com.psyweb.client.domain.Client;
import com.psyweb.client.repository.ClientRepository;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.service.UserService;

import jakarta.transaction.Transactional;

import com.psyweb.client.exception.ClientNotFoundException;
import com.psyweb.client.exception.InvalidClientDataException;

@Service
public class ClientService {
	private final ClientRepository clientRepository;
	private final UserService userService;

	public ClientService(ClientRepository clientRepository, UserService userService) {
		this.clientRepository = clientRepository;
		this.userService = userService;
	}

	@Transactional
	public Client createClient(User user, String firstName, String lastName) {
		if (user == null || user.getStatus() != UserStatus.ACTIVE || user.getRole() != UserRole.CLIENT) {
			throw new InvalidClientDataException("Invalid user");
		}
		Client client = new Client(user, firstName, lastName);
		return clientRepository.save(client);
	}

	public Client getClient(Long clientId) {
		if (clientId == null) {
			throw new InvalidClientDataException("Client id cannot be null");
		}
		return clientRepository.findById(clientId).orElseThrow(() -> new ClientNotFoundException("Client not found"));
	}

	public Client getActiveClient(Long clientId) {
		userService.getActiveUser(clientId);
		return getClient(clientId);
	}
}
