
package com.psyweb.auth.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

class RefreshTokenTest {

    private RefreshSession session;
    private Instant createdAt;
    private Instant expiresAt;
    private String tokenHash;

    @BeforeEach
    void setUp() {
        User user = new User(
                "client@example.com",
                "valid-password-hash",
                UserRole.CLIENT,
                UserStatus.ACTIVE
        );

        createdAt = Instant.parse("2026-10-23T10:00:00Z");
        expiresAt = createdAt.plusSeconds(3600);
        tokenHash = "a".repeat(64);

        session = new RefreshSession(user, createdAt, expiresAt);
    }

    private RefreshToken createToken() {
        return new RefreshToken(session, tokenHash, createdAt);
    }

    @Test
    void shouldCreateValidToken() {
        RefreshToken token = createToken();

        assertEquals(tokenHash, token.getTokenHash());
        assertEquals(createdAt, token.getIssuedAt());
        assertNull(token.getConsumedAt());
    }

    @Test
    void shouldRejectInvalidTokenHash() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new RefreshToken(session, null, createdAt)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new RefreshToken(session, "short", createdAt))
        );
    }

    @Test
    void shouldRejectNullSession() {
        assertThrows(IllegalArgumentException.class,
                () -> new RefreshToken(null, tokenHash, createdAt));
    }

    @Test
    void shouldRejectIssuedAtOutsideSessionLifetime() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new RefreshToken(session, tokenHash, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new RefreshToken(
                                session, tokenHash, createdAt.minusSeconds(1))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new RefreshToken(session, tokenHash, expiresAt))
        );
    }

    @Test
    void shouldRejectTokenCreationForRevokedSession() {
        session.addRevokedAt(createdAt.plusSeconds(30));

        assertThrows(IllegalArgumentException.class,
                () -> new RefreshToken(
                        session, tokenHash, createdAt.plusSeconds(60)));
    }

    @Test
    void shouldConsumeToken() {
        RefreshToken token = createToken();
        Instant consumedAt = createdAt.plusSeconds(60);

        token.consumeToken(consumedAt);

        assertEquals(consumedAt, token.getConsumedAt());
    }

    @Test
    void shouldRejectRepeatedConsumption() {
        RefreshToken token = createToken();
        Instant firstConsumption = createdAt.plusSeconds(60);

        token.consumeToken(firstConsumption);

        assertThrows(IllegalArgumentException.class,
                () -> token.consumeToken(createdAt.plusSeconds(120)));

        assertEquals(firstConsumption, token.getConsumedAt());
    }

    @Test
    void shouldRejectConsumptionBeforeIssuance() {
        RefreshToken token = createToken();

        assertThrows(IllegalArgumentException.class,
                () -> token.consumeToken(createdAt.minusSeconds(1)));

        assertNull(token.getConsumedAt());
    }

    @Test
    void shouldRejectConsumptionAfterSessionExpiration() {
        RefreshToken token = createToken();

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> token.consumeToken(expiresAt)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> token.consumeToken(expiresAt.plusSeconds(1)))
        );

        assertNull(token.getConsumedAt());
    }

    @Test
    void shouldRejectConsumptionAfterSessionRevocation() {
        RefreshToken token = createToken();
        session.addRevokedAt(createdAt.plusSeconds(60));

        assertThrows(IllegalArgumentException.class,
                () -> token.consumeToken(createdAt.plusSeconds(120)));

        assertNull(token.getConsumedAt());
    }

    @Test
    void shouldRejectNullConsumptionTime() {
        RefreshToken token = createToken();

        assertThrows(IllegalArgumentException.class,
                () -> token.consumeToken(null));
    }
}