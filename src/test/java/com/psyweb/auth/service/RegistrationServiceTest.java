
package com.psyweb.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.time.Duration;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.psyweb.auth.exception.InvalidPasswordException;
import com.psyweb.client.domain.Client;
import com.psyweb.client.service.ClientService;
import com.psyweb.common.config.SpecialistDefaultsProperties;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.service.SpecialistService;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.exception.UserEmailAlreadyExistsException;
import com.psyweb.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserService userService;

    @Mock
    private ClientService clientService;

    @Mock
    private SpecialistService specialistService;

    @Mock
    private SpecialistDefaultsProperties specialistDefaults;

    private RegistrationService registrationService;

    private static final String EMAIL = "testemail@example.com";
    private static final String PASSWORD = "ValidPassword12345!";
    private static final String HASH = "encoded-password-hash";
    private static final String FIRST_NAME = "Anna";
    private static final String LAST_NAME = "Smith";

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(
                passwordEncoder,
                userService,
                clientService,
                specialistService,
                specialistDefaults
        );
    }

    @Test
    void shouldRegisterClientWithEncodedPassword() {
        User user = new User(EMAIL, HASH, UserRole.CLIENT, UserStatus.ACTIVE);
        Client client = new Client(user, FIRST_NAME, LAST_NAME);

        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userService.createUser(EMAIL, HASH, UserRole.CLIENT)).thenReturn(user);
        when(clientService.createClient(user, FIRST_NAME, LAST_NAME)).thenReturn(client);

        Client result = registrationService.registerClient(
                EMAIL, PASSWORD, FIRST_NAME, LAST_NAME
        );

        assertSame(client, result);
        verify(passwordEncoder).encode(PASSWORD);
        verify(userService).createUser(EMAIL, HASH, UserRole.CLIENT);
        verify(clientService).createClient(user, FIRST_NAME, LAST_NAME);
        verifyNoInteractions(specialistService);
    }

    @Test
    void shouldRegisterSpecialistWithConfiguredNotices() {
        User user = new User(
                EMAIL, HASH, UserRole.SPECIALIST, UserStatus.ACTIVE
        );

        Duration bookingNotice = Duration.ofHours(2);
        Duration cancellationNotice = Duration.ofHours(24);

        Specialist specialist = new Specialist(
                user, FIRST_NAME, LAST_NAME,
                bookingNotice, cancellationNotice
        );

        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userService.createUser(EMAIL, HASH, UserRole.SPECIALIST))
                .thenReturn(user);

        when(specialistDefaults.minimumBookingNotice())
                .thenReturn(bookingNotice);
        when(specialistDefaults.clientCancellationNotice())
                .thenReturn(cancellationNotice);

        when(specialistService.createSpecialist(
                user,
                FIRST_NAME,
                LAST_NAME,
                bookingNotice,
                cancellationNotice
        )).thenReturn(specialist);

        Specialist result = registrationService.registerSpecialist(
                EMAIL, PASSWORD, FIRST_NAME, LAST_NAME
        );

        assertSame(specialist, result);
        verify(userService).createUser(EMAIL, HASH, UserRole.SPECIALIST);

        verify(specialistService).createSpecialist(
                user,
                FIRST_NAME,
                LAST_NAME,
                bookingNotice,
                cancellationNotice
        );

        verifyNoInteractions(clientService);
    }

    @Test
    void shouldNormalizeEmailBeforeCreatingUser() {
        User user = new User(EMAIL, HASH, UserRole.CLIENT, UserStatus.ACTIVE);
        Client client = new Client(user, FIRST_NAME, LAST_NAME);

        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userService.createUser(EMAIL, HASH, UserRole.CLIENT)).thenReturn(user);
        when(clientService.createClient(user, FIRST_NAME, LAST_NAME)).thenReturn(client);

        Client result = registrationService.registerClient(
                "  TestEmail@Example.COM  ",
                PASSWORD,
                FIRST_NAME,
                LAST_NAME
        );

        assertSame(client, result);
        verify(userService).createUser(EMAIL, HASH, UserRole.CLIENT);
    }

    @Test
    void shouldRejectInvalidPasswordWithoutCreatingAccount() {
        assertThrows(
                InvalidPasswordException.class,
                () -> registrationService.registerClient(
                        EMAIL, "short", FIRST_NAME, LAST_NAME
                )
        );

        verifyNoInteractions(
                passwordEncoder,
                userService,
                clientService,
                specialistService
        );
    }

    @Test
    void shouldPropagateEmailAlreadyExistsException() {
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);

        when(userService.createUser(EMAIL, HASH, UserRole.CLIENT))
                .thenThrow(new UserEmailAlreadyExistsException("Duplicate"));

        assertThrows(
                UserEmailAlreadyExistsException.class,
                () -> registrationService.registerClient(
                        EMAIL, PASSWORD, FIRST_NAME, LAST_NAME
                )
        );

        verifyNoInteractions(clientService, specialistService);
    }

    @Test
    void shouldTranslateEmailUniqueConstraintViolation() {
        DataIntegrityViolationException exception =
                constraintFailure("23505", "users_email_key");

        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userService.createUser(EMAIL, HASH, UserRole.CLIENT))
                .thenThrow(exception);

        UserEmailAlreadyExistsException result = assertThrows(
                UserEmailAlreadyExistsException.class,
                () -> registrationService.registerClient(
                        EMAIL, PASSWORD, FIRST_NAME, LAST_NAME
                )
        );

        assertSame(exception, result.getCause());
        verifyNoInteractions(clientService);
    }

    @Test
    void shouldPropagateUnrelatedDataIntegrityViolation() {
        DataIntegrityViolationException exception =
                constraintFailure(
                        "23502", "users_password_hash_not_null"
                );

        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userService.createUser(EMAIL, HASH, UserRole.CLIENT))
                .thenThrow(exception);

        DataIntegrityViolationException result = assertThrows(
                DataIntegrityViolationException.class,
                () -> registrationService.registerClient(
                        EMAIL, PASSWORD, FIRST_NAME, LAST_NAME
                )
        );

        assertSame(exception, result);
    }

    @Test
    void shouldNotTranslateWrongSqlStateForEmailConstraint() {
        DataIntegrityViolationException exception =
                constraintFailure("23514", "users_email_key");

        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userService.createUser(EMAIL, HASH, UserRole.CLIENT))
                .thenThrow(exception);

        DataIntegrityViolationException result = assertThrows(
                DataIntegrityViolationException.class,
                () -> registrationService.registerClient(
                        EMAIL, PASSWORD, FIRST_NAME, LAST_NAME
                )
        );

        assertSame(exception, result);
    }

    private static DataIntegrityViolationException constraintFailure(
            String sqlState,
            String constraintName
    ) {
        SQLException cause = new SQLException(
                "Constraint violation", sqlState
        );

        ConstraintViolationException hibernateException =
                new ConstraintViolationException(
                        "Constraint violation", cause, constraintName
                );

        return new DataIntegrityViolationException(
                "Database constraint violation",
                hibernateException
        );
    }
}
