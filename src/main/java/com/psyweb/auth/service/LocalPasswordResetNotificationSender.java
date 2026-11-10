
package com.psyweb.auth.service;

import java.time.Instant;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalPasswordResetNotificationSender
        implements PasswordResetNotificationSender {

    @Override
    public void sendPasswordReset(
            String email,
            String rawToken,
            Instant expiresAt
    ) {
        // Email delivery is not implemented for local development.
    }
}