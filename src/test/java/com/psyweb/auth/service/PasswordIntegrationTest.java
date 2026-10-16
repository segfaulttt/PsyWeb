
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.psyweb.auth.exception.InvalidCurrentPasswordException;
import com.psyweb.client.domain.Client;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.exception.ConcurrentPasswordChangeException;
import com.psyweb.user.repository.UserRepository;
import com.psyweb.user.service.UserService;

class PasswordIntegrationTest extends PostgreSQLIntegrationTest {

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String CURRENT_PASSWORD = "CurrentPassword123!";
    private static final String NEW_PASSWORD = "NewStrongPassword123!";

    private String uniqueEmail() {
        return "password-" + UUID.randomUUID() + "@example.com";
    }

    private User registerTestClient() {
        Client client = registrationService.registerClient(
                uniqueEmail(),
                CURRENT_PASSWORD,
                "Test",
                "Client"
        );

        return userRepository.findById(client.getId()).orElseThrow();
    }

    @Test
    void shouldPersistNewPasswordHash() {
        User registered = registerTestClient();

        Long userId = registered.getId();
        String previousHash = registered.getPasswordHash();

        passwordService.changePassword(
                userId,
                CURRENT_PASSWORD,
                NEW_PASSWORD
        );

        User persisted = userRepository.findById(userId).orElseThrow();

        assertNotEquals(previousHash, persisted.getPasswordHash());

        assertTrue(
                passwordEncoder.matches(
                        NEW_PASSWORD,
                        persisted.getPasswordHash()
                )
        );

        assertFalse(
                passwordEncoder.matches(
                        CURRENT_PASSWORD,
                        persisted.getPasswordHash()
                )
        );

        assertTrue(persisted.getPasswordHash().startsWith("$argon2id$"));
    }

    @Test
    void shouldKeepPasswordUnchangedWhenCurrentPasswordIsWrong() {
        User registered = registerTestClient();

        String previousHash = registered.getPasswordHash();

        assertThrows(
                InvalidCurrentPasswordException.class,
                () -> passwordService.changePassword(
                        registered.getId(),
                        "IncorrectPassword123!",
                        NEW_PASSWORD
                )
        );

        User persisted = userRepository.findById(registered.getId())
                .orElseThrow();

        assertEquals(previousHash, persisted.getPasswordHash());
    }

    @Test
    void shouldRejectStalePasswordHashWithoutOverwriting() {
        User registered = registerTestClient();

        Long userId = registered.getId();
        String oldHash = registered.getPasswordHash();

        passwordService.changePassword(
                userId,
                CURRENT_PASSWORD,
                NEW_PASSWORD
        );

        User updated = userRepository.findById(userId).orElseThrow();
        String updatedHash = updated.getPasswordHash();

        assertThrows(
                ConcurrentPasswordChangeException.class,
                () -> userService.changePassword(
                        userId,
                        oldHash,
                        "another-encoded-hash"
                )
        );

        User persisted = userRepository.findById(userId).orElseThrow();

        assertEquals(updatedHash, persisted.getPasswordHash());

        assertTrue(
                passwordEncoder.matches(
                        NEW_PASSWORD,
                        persisted.getPasswordHash()
                )
        );
    }

    @Test
    void shouldPreventConcurrentPasswordChanges() throws Exception {
        User registered = registerTestClient();

        Long userId = registered.getId();
        String oldHash = registered.getPasswordHash();

        String firstNewHash =
                passwordEncoder.encode("FirstNewPassword123!");

        String secondNewHash =
                passwordEncoder.encode("SecondNewPassword123!");

        CountDownLatch startLatch = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {

            Future<Object> first = executor.submit(() -> {
                startLatch.await();

                try {
                    return userService.changePassword(
                            userId,
                            oldHash,
                            firstNewHash
                    );
                } catch (ConcurrentPasswordChangeException e) {
                    return e;
                }
            });

            Future<Object> second = executor.submit(() -> {
                startLatch.await();

                try {
                    return userService.changePassword(
                            userId,
                            oldHash,
                            secondNewHash
                    );
                } catch (ConcurrentPasswordChangeException e) {
                    return e;
                }
            });

            startLatch.countDown();

            Object firstResult = first.get(30, TimeUnit.SECONDS);
            Object secondResult = second.get(30, TimeUnit.SECONDS);

            int successfulChanges = 0;
            int conflicts = 0;

            for (Object result : new Object[]{firstResult, secondResult}) {
                if (result instanceof User) {
                    successfulChanges++;
                } else if (result instanceof ConcurrentPasswordChangeException) {
                    conflicts++;
                } else {
                    fail("Unexpected result: " + result);
                }
            }

            assertEquals(1, successfulChanges);
            assertEquals(1, conflicts);

            User persisted = userRepository.findById(userId).orElseThrow();

            boolean firstPasswordMatches = passwordEncoder.matches(
                    "FirstNewPassword123!",
                    persisted.getPasswordHash()
            );

            boolean secondPasswordMatches = passwordEncoder.matches(
                    "SecondNewPassword123!",
                    persisted.getPasswordHash()
            );

            assertTrue(firstPasswordMatches ^ secondPasswordMatches);

            assertFalse(
                    passwordEncoder.matches(
                            CURRENT_PASSWORD,
                            persisted.getPasswordHash()
                    )
            );
        }
    }
}
