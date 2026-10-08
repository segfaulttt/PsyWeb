package com.psyweb.client.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.psyweb.client.domain.Client;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

public class ClientRepositoryIntegrationTest extends PostgreSQLIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	public void shouldPersistAndRestoreClient() {
		User user = userRepository
				.saveAndFlush(new User("client-persistence@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));
		Long userId = user.getId();

		Client client = new Client(user, "Anna", "Smith");

		clientRepository.saveAndFlush(client);

		entityManager.clear();

		Client restored = clientRepository.findById(userId).orElseThrow();

		assertEquals(userId, restored.getId());
		assertEquals("Anna", restored.getFirstName());
		assertEquals("Smith", restored.getLastName());
	}
}