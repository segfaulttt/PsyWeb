
package com.psyweb.auth.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

class RefreshSessionTest {

    private User user;
    private Instant createdAt;
    private Instant expiresAt;

    @BeforeEach
    void setUp() {
        user = new User(
                "client@example.com",
                "valid-password-hash",
                UserRole.CLIENT,
                UserStatus.ACTIVE
        );

        createdAt = Instant.parse("2026-10-23T10:00:00Z");
        expiresAt = createdAt.plusSeconds(3600);
    }

    private RefreshSession createSession() {
        return new RefreshSession(user, createdAt, expiresAt);
    }

    @Test
    void shouldCreateValidSession() {
        RefreshSession session = createSession();

        assertEquals(createdAt, session.getCreatedAt());
        assertEquals(expiresAt, session.getExpiresAt());
        assertNull(session.getRevokedAt());
        assertNull(session.getLastUsedAt());
    }

    @Test
    void shouldRejectNullUser() {
        assertThrows(IllegalArgumentException.class,
                () -> new RefreshSession(null, createdAt, expiresAt));
    }

    @Test
    void shouldRejectInvalidCreationTimeRange() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new RefreshSession(user, createdAt, createdAt)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new RefreshSession(user, expiresAt, createdAt)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new RefreshSession(user, null, expiresAt)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new RefreshSession(user, createdAt, null))
        );
    }

    @Test
    void shouldUpdateLastUsedAt() {
        RefreshSession session = createSession();
        Instant usageTime = createdAt.plusSeconds(60);

        session.addLastUsedAt(usageTime);

        assertEquals(usageTime, session.getLastUsedAt());
    }

    @Test
    void shouldAllowEqualLastUsedAt() {
        RefreshSession session = createSession();
        Instant usageTime = createdAt.plusSeconds(60);

        session.addLastUsedAt(usageTime);

        assertDoesNotThrow(() -> session.addLastUsedAt(usageTime));
        assertEquals(usageTime, session.getLastUsedAt());
    }

    @Test
    void shouldRejectDecreasingLastUsedAt() {
        RefreshSession session = createSession();
        Instant firstUsage = createdAt.plusSeconds(120);
        session.addLastUsedAt(firstUsage);

        assertThrows(IllegalArgumentException.class,
                () -> session.addLastUsedAt(createdAt.plusSeconds(60)));

        assertEquals(firstUsage, session.getLastUsedAt());
    }

    @Test
    void shouldRejectUsageOutsideSessionLifetime() {
        RefreshSession session = createSession();

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> session.addLastUsedAt(createdAt.minusSeconds(1))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> session.addLastUsedAt(expiresAt)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> session.addLastUsedAt(expiresAt.plusSeconds(1))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> session.addLastUsedAt(null))
        );
    }

    @Test
    void shouldRevokeSession() {
        RefreshSession session = createSession();
        Instant revokedAt = createdAt.plusSeconds(120);

        session.addRevokedAt(revokedAt);

        assertEquals(revokedAt, session.getRevokedAt());
    }

    @Test
    void shouldRejectRepeatedRevocation() {
        RefreshSession session = createSession();
        Instant revokedAt = createdAt.plusSeconds(120);
        session.addRevokedAt(revokedAt);

        assertThrows(IllegalArgumentException.class,
                () -> session.addRevokedAt(revokedAt.plusSeconds(1)));

        assertEquals(revokedAt, session.getRevokedAt());
    }

    @Test
    void shouldRejectRevocationBeforeCreation() {
        RefreshSession session = createSession();

        assertThrows(IllegalArgumentException.class,
                () -> session.addRevokedAt(createdAt.minusSeconds(1)));
    }

    @Test
    void shouldRejectRevocationBeforeLastUsage() {
        RefreshSession session = createSession();
        session.addLastUsedAt(createdAt.plusSeconds(120));

        assertThrows(IllegalArgumentException.class,
                () -> session.addRevokedAt(createdAt.plusSeconds(60)));

        assertNull(session.getRevokedAt());
    }

    @Test
    void shouldRejectUsageAfterRevocation() {
        RefreshSession session = createSession();
        session.addRevokedAt(createdAt.plusSeconds(120));

        assertThrows(IllegalArgumentException.class,
                () -> session.addLastUsedAt(createdAt.plusSeconds(180)));
    }
}