package com.psyweb.client.web.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.psyweb.client.domain.Client;
import com.psyweb.client.web.dto.response.ClientProfileResponse;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

class ClientWebMapperTest {

	private final ClientWebMapper mapper = new ClientWebMapper();

	@Test
	void shouldMapClientToProfileResponse() {
		User user = new User("client@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE);

		Client client = new Client(user, "Anna", "Smith");

		ClientProfileResponse response = mapper.toProfileResponse(client);

		assertEquals(client.getId(), response.id());
		assertEquals("Anna", response.firstName());
		assertEquals("Smith", response.lastName());
	}
}