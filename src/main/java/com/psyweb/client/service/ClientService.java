package com.psyweb.client.service;

import org.springframework.stereotype.Service;

import com.psyweb.client.domain.Client;
import com.psyweb.client.repository.ClientRepository;
import com.psyweb.user.service.UserService;

import jakarta.transaction.Transactional;

import com.psyweb.client.exception.ClientNotFoundException;
import com.psyweb.client.exception.InvalidClientDataException;


@Service
public class ClientService {
	private final ClientRepository clientRepository;
	private UserService userService;
	
	public ClientService(ClientRepository clientRepository, UserService userService) {
		this.clientRepository = clientRepository;
		this.userService = userService;
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
	
//	@Transactional
//	public Client createClient(User user, String firstName, String lastName) {
//		
//	}
}
