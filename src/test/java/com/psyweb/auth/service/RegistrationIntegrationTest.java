
package com.psyweb.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.psyweb.client.domain.Client;
import com.psyweb.client.repository.ClientRepository;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.domain.SpecialistStatus;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.exception.UserEmailAlreadyExistsException;
import com.psyweb.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

class RegistrationIntegrationTest extends PostgreSQLIntegrationTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String uniqueEmail() {
        return "registration-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void shouldPersistClientRegistrationWithArgon2PasswordHash() {
        String email = uniqueEmail();
        String password = "VeryStrongPassword123!";

        Client client = registrationService.registerClient(
                email, password, "Anna", "Smith"
        );

        assertNotNull(client.getId());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.clear();

            User user = userRepository.findById(client.getId()).orElseThrow();
            Client persisted = clientRepository.findById(client.getId()).orElseThrow();

            assertEquals(email, user.getEmail());
            assertEquals(UserRole.CLIENT, user.getRole());
            assertEquals(UserStatus.ACTIVE, user.getStatus());
            assertNull(user.getEmailVerifiedAt());

            assertEquals("Anna", persisted.getFirstName());
            assertEquals("Smith", persisted.getLastName());

            assertTrue(user.getPasswordHash().startsWith("$argon2id$"));
            assertTrue(passwordEncoder.matches(password, user.getPasswordHash()));
            assertFalse(passwordEncoder.matches(
                    "WrongPassword123!", user.getPasswordHash()
            ));

            assertFalse(specialistRepository.existsById(user.getId()));
        });
    }

    @Test
    void shouldPersistSpecialistRegistrationWithPendingStatus() {
        String email = uniqueEmail();

        Specialist specialist = registrationService.registerSpecialist(
                email,
                "VeryStrongPassword123!",
                "Kate",
                "Jones"
        );

        assertNotNull(specialist.getId());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.clear();

            User user = userRepository.findById(specialist.getId()).orElseThrow();
            Specialist persisted =
                    specialistRepository.findById(specialist.getId()).orElseThrow();

            assertEquals(UserRole.SPECIALIST, user.getRole());
            assertEquals(UserStatus.ACTIVE, user.getStatus());

            assertEquals(
                    SpecialistStatus.PENDING,
                    persisted.getApprovalStatus()
            );

            assertEquals("Kate", persisted.getFirstName());
            assertEquals("Jones", persisted.getLastName());

            assertNotNull(persisted.getMinimumBookingNotice());
            assertNotNull(persisted.getClientCancellationNotice());

            assertFalse(clientRepository.existsById(user.getId()));
        });
    }

    @Test
    void shouldRollbackUserWhenClientProfileValidationFails() {
        String email = uniqueEmail();

        assertThrows(
                RuntimeException.class,
                () -> registrationService.registerClient(
                        email, "ValidPassword123!", " ", "Smith"
                )
        );

        assertTrue(
                userRepository.findByEmail(email).isEmpty(),
                "User must not persist when Client creation fails"
        );
    }

    @Test
    void shouldRollbackUserWhenSpecialistProfileValidationFails() {
        String email = uniqueEmail();

        assertThrows(
                RuntimeException.class,
                () -> registrationService.registerSpecialist(
                        email, "ValidPassword123!", " ", "Smith"
                )
        );

        assertTrue(
                userRepository.findByEmail(email).isEmpty(),
                "User must not persist when Specialist creation fails"
        );
    }

    @Test
    void shouldRejectDuplicateEmailAcrossAccountTypes() {
        String email = uniqueEmail();

        Client client = registrationService.registerClient(
                email, "ValidPassword123!", "Anna", "Smith"
        );

        assertThrows(
                UserEmailAlreadyExistsException.class,
                () -> registrationService.registerSpecialist(
                        email, "ValidPassword123!", "Kate", "Jones"
                )
        );

        assertNotNull(client.getId());

        assertEquals(
                client.getId(),
                userRepository.findByEmail(email).orElseThrow().getId()
        );

        assertTrue(clientRepository.existsById(client.getId()));
        assertFalse(specialistRepository.existsById(client.getId()));
    }

    @Test
    void shouldRejectDuplicateEmailInDatabase() {
        String email = uniqueEmail();

        userRepository.saveAndFlush(
                new User(
                        email, "hash-1",
                        UserRole.CLIENT, UserStatus.ACTIVE
                )
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(
                        new User(
                                email, "hash-2",
                                UserRole.CLIENT, UserStatus.ACTIVE
                        )
                )
        );
    }

    @Test
    void shouldAllowOnlyOneConcurrentRegistrationForSameEmail() throws Exception {
        String email = uniqueEmail();
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Object> first = executor.submit(
                    () -> registerConcurrently(start, email)
            );

            Future<Object> second = executor.submit(
                    () -> registerConcurrently(start, email)
            );

            start.countDown();

            Object a = first.get(30, TimeUnit.SECONDS);
            Object b = second.get(30, TimeUnit.SECONDS);

            int successes = 0;
            int duplicates = 0;

            for (Object value : new Object[]{a, b}) {
                if (value instanceof Client) {
                    successes++;
                } else if (value instanceof UserEmailAlreadyExistsException) {
                    duplicates++;
                } else {
                    fail("Unexpected registration result: " + value);
                }
            }

            assertEquals(1, successes);
            assertEquals(1, duplicates);

            User persisted =
                    userRepository.findByEmail(email).orElseThrow();

            assertTrue(clientRepository.existsById(persisted.getId()));
        }
    }

    private Object registerConcurrently(
            CountDownLatch start,
            String email
    ) throws InterruptedException {
        start.await();

        try {
            return registrationService.registerClient(
                    email,
                    "ConcurrentPassword123!",
                    "Anna",
                    "Smith"
            );
        } catch (UserEmailAlreadyExistsException ex) {
            return ex;
        }
    }
}
