
package com.psyweb.client.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.psyweb.client.domain.Client;
import com.psyweb.client.exception.ClientNotFoundException;
import com.psyweb.client.exception.InvalidClientDataException;
import com.psyweb.client.repository.ClientRepository;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
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
    void shouldCreateClientForActiveClientUser() {
        User user = user(UserRole.CLIENT, UserStatus.ACTIVE);

        when(clientRepository.save(any(Client.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Client result = clientService.createClient(
                user, "Anna", "Smith"
        );

        assertEquals("Anna", result.getFirstName());
        assertEquals("Smith", result.getLastName());

        verify(clientRepository).save(same(result));
        verifyNoInteractions(userService);
    }

    @Test
    void shouldRejectNullUserOnCreation() {
        assertThrows(
                InvalidClientDataException.class,
                () -> clientService.createClient(
                        null, "Anna", "Smith"
                )
        );

        verifyNoInteractions(clientRepository);
    }

    @Test
    void shouldRejectSpecialistUserOnClientCreation() {
        assertThrows(
                InvalidClientDataException.class,
                () -> clientService.createClient(
                        user(UserRole.SPECIALIST, UserStatus.ACTIVE),
                        "Anna",
                        "Smith"
                )
        );

        verifyNoInteractions(clientRepository);
    }

    @Test
    void shouldRejectBlockedUserOnClientCreation() {
        assertThrows(
                InvalidClientDataException.class,
                () -> clientService.createClient(
                        user(UserRole.CLIENT, UserStatus.BLOCKED),
                        "Anna",
                        "Smith"
                )
        );

        verifyNoInteractions(clientRepository);
    }

    @Test
    void shouldRejectBlankFirstNameOnCreation() {
        assertThrows(
                InvalidClientDataException.class,
                () -> clientService.createClient(
                        user(UserRole.CLIENT, UserStatus.ACTIVE),
                        " ",
                        "Smith"
                )
        );

        verifyNoInteractions(clientRepository);
    }

    @Test
    void shouldReturnClientById() {
        Client client = mock(Client.class);

        when(clientRepository.findById(1L))
                .thenReturn(Optional.of(client));

        assertSame(client, clientService.getClient(1L));
        verify(clientRepository).findById(1L);
    }

    @Test
    void shouldRejectNullClientId() {
        assertThrows(
                InvalidClientDataException.class,
                () -> clientService.getClient(null)
        );
    }

    @Test
    void shouldThrowWhenClientDoesNotExist() {
        when(clientRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ClientNotFoundException.class,
                () -> clientService.getClient(1L)
        );
    }

    @Test
    void shouldReturnActiveClient() {
        Client client = mock(Client.class);

        when(clientRepository.findById(1L))
                .thenReturn(Optional.of(client));

        assertSame(client, clientService.getActiveClient(1L));

        verify(userService).getActiveUser(1L);
        verify(clientRepository).findById(1L);
    }

    private static User user(UserRole role, UserStatus status) {
        return new User(
                "client@example.com", "hash", role, status
        );
    }
}
